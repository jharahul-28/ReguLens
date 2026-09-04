package com.rj.ReguLens.retrieval.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rj.ReguLens.entity.DocumentChunk;
import com.rj.ReguLens.entity.Policy;
import com.rj.ReguLens.entity.PolicyVersion;
import com.rj.ReguLens.entity.PolicyVersionStatusEnum;
import com.rj.ReguLens.repository.DocumentChunkRepository;
import com.rj.ReguLens.retrieval.RetrievalPort;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class NeonPgVectorRetrievalAdapter implements RetrievalPort {

    EntityManager entityManager;
    DocumentChunkRepository documentChunkRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    private static final double RRF_K = 60.0;
    private static final double WEIGHT_VECTOR = 0.70;
    private static final double WEIGHT_FTS = 0.30;

    @Override
    public List<ScoredDocumentChunk> searchVector(float[] queryVector, RetrievalFilter filter) {
        String vectorString = formatVectorToString(queryVector);

        String sql = """
            SELECT 
                dc.id AS chunk_id,
                dc.policy_version_id,
                pv.policy_id,
                p.title AS policy_title,
                pv.version AS version_number,
                pv.status AS policy_status,
                pv.effective_from,
                pv.effective_to,
                dc.clause_reference,
                dc.section_hierarchy,
                dc.content,
                GREATEST(0.0, 1.0 - (dc.embedding <=> cast(:vectorString as vector))) AS vector_similarity,
                0.0 AS fts_score,
                dc.metadata
            FROM document_chunk dc
            JOIN policy_version pv ON dc.policy_version_id = pv.id
            JOIN policy p ON pv.policy_id = p.id
            WHERE (cast(:jurisdiction as text) IS NULL OR cast(:jurisdiction as text) = 'GLOBAL' OR dc.metadata->>'jurisdiction' = cast(:jurisdiction as text) OR dc.metadata->>'jurisdiction' = 'GLOBAL')
              AND (cast(:regulationName as text) IS NULL OR dc.metadata->>'regulation_name' = cast(:regulationName as text))
              AND (cast(:policyId as uuid) IS NULL OR p.id = cast(:policyId as uuid))
              AND (cast(:policyVersionId as uuid) IS NULL OR pv.id = cast(:policyVersionId as uuid))
              AND (cast(:onlyActive as boolean) = false OR pv.status = 2)
            ORDER BY dc.embedding <=> cast(:vectorString as vector) ASC
            LIMIT :maxResults
        """;

        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("vectorString", vectorString);
            query.setParameter("jurisdiction", filter.getJurisdiction());
            query.setParameter("regulationName", filter.getRegulationName());
            query.setParameter("policyId", filter.getPolicyId());
            query.setParameter("policyVersionId", filter.getPolicyVersionId());
            query.setParameter("onlyActive", filter.isOnlyActive());
            query.setParameter("maxResults", filter.getMaxResults());

            List<?> results = query.getResultList();
            return mapSqlResultsToScoredChunks(results, true);
        } catch (Exception e) {
            log.warn("Native pgvector query failed ({}), falling back to in-memory cosine ranking.", e.getMessage());
            return inMemoryFallbackSearch(queryVector, null, filter, false);
        }
    }

    @Override
    public List<ScoredDocumentChunk> searchHybrid(float[] queryVector, String queryText, RetrievalFilter filter) {
        String vectorString = formatVectorToString(queryVector);

        String sql = """
            WITH vector_matches AS (
                SELECT 
                    dc.id AS chunk_id,
                    dc.policy_version_id,
                    pv.policy_id,
                    p.title AS policy_title,
                    pv.version AS version_number,
                    pv.status AS policy_status,
                    pv.effective_from,
                    pv.effective_to,
                    dc.clause_reference,
                    dc.section_hierarchy,
                    dc.content,
                    GREATEST(0.0, 1.0 - (dc.embedding <=> cast(:vectorString as vector))) AS vector_score,
                    dc.metadata,
                    ROW_NUMBER() OVER (ORDER BY dc.embedding <=> cast(:vectorString as vector) ASC) AS vec_rank
                FROM document_chunk dc
                JOIN policy_version pv ON dc.policy_version_id = pv.id
                JOIN policy p ON pv.policy_id = p.id
                WHERE (cast(:jurisdiction as text) IS NULL OR cast(:jurisdiction as text) = 'GLOBAL' OR dc.metadata->>'jurisdiction' = cast(:jurisdiction as text) OR dc.metadata->>'jurisdiction' = 'GLOBAL')
                  AND (cast(:regulationName as text) IS NULL OR dc.metadata->>'regulation_name' = cast(:regulationName as text))
                  AND (cast(:policyId as uuid) IS NULL OR p.id = cast(:policyId as uuid))
                  AND (cast(:policyVersionId as uuid) IS NULL OR pv.id = cast(:policyVersionId as uuid))
                  AND (cast(:onlyActive as boolean) = false OR pv.status = 2)
                LIMIT 50
            ),
            fts_matches AS (
                SELECT 
                    dc.id AS chunk_id,
                    GREATEST(
                        ts_rank_cd(dc.content_tsv, plainto_tsquery('english', cast(:queryText as text))),
                        CASE 
                            WHEN plainto_tsquery('english', cast(:queryText as text))::text <> '' 
                            THEN ts_rank_cd(dc.content_tsv, to_tsquery('english', regexp_replace(plainto_tsquery('english', cast(:queryText as text))::text, '&', '|', 'g')))
                            ELSE 0.0 
                        END
                    ) AS fts_score,
                    ROW_NUMBER() OVER (
                        ORDER BY GREATEST(
                            ts_rank_cd(dc.content_tsv, plainto_tsquery('english', cast(:queryText as text))),
                            CASE 
                                WHEN plainto_tsquery('english', cast(:queryText as text))::text <> '' 
                                THEN ts_rank_cd(dc.content_tsv, to_tsquery('english', regexp_replace(plainto_tsquery('english', cast(:queryText as text))::text, '&', '|', 'g')))
                                ELSE 0.0 
                            END
                        ) DESC
                    ) AS fts_rank
                FROM document_chunk dc
                JOIN policy_version pv ON dc.policy_version_id = pv.id
                JOIN policy p ON pv.policy_id = p.id
                WHERE (
                    dc.content_tsv @@ plainto_tsquery('english', cast(:queryText as text))
                    OR (
                        plainto_tsquery('english', cast(:queryText as text))::text <> '' 
                        AND dc.content_tsv @@ to_tsquery('english', regexp_replace(plainto_tsquery('english', cast(:queryText as text))::text, '&', '|', 'g'))
                    )
                )
                  AND (cast(:jurisdiction as text) IS NULL OR cast(:jurisdiction as text) = 'GLOBAL' OR dc.metadata->>'jurisdiction' = cast(:jurisdiction as text) OR dc.metadata->>'jurisdiction' = 'GLOBAL')
                  AND (cast(:regulationName as text) IS NULL OR dc.metadata->>'regulation_name' = cast(:regulationName as text))
                  AND (cast(:policyId as uuid) IS NULL OR p.id = cast(:policyId as uuid))
                  AND (cast(:policyVersionId as uuid) IS NULL OR pv.id = cast(:policyVersionId as uuid))
                  AND (cast(:onlyActive as boolean) = false OR pv.status = 2)
                LIMIT 50
            )
            SELECT 
                vm.chunk_id,
                vm.policy_version_id,
                vm.policy_id,
                vm.policy_title,
                vm.version_number,
                vm.policy_status,
                vm.effective_from,
                vm.effective_to,
                vm.clause_reference,
                vm.section_hierarchy,
                vm.content,
                vm.vector_score,
                COALESCE(fm.fts_score, 0.0) AS fts_score,
                (
                    (cast(:weightVector as double precision) / (cast(:rrfK as double precision) + vm.vec_rank)) + 
                    COALESCE(cast(:weightFts as double precision) / (cast(:rrfK as double precision) + fm.fts_rank), 0.0)
                ) AS rrf_score,
                vm.metadata
            FROM vector_matches vm
            LEFT JOIN fts_matches fm ON vm.chunk_id = fm.chunk_id
            ORDER BY rrf_score DESC
            LIMIT :maxResults
        """;

        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("vectorString", vectorString);
            query.setParameter("queryText", queryText != null ? queryText : "");
            query.setParameter("jurisdiction", filter.getJurisdiction());
            query.setParameter("regulationName", filter.getRegulationName());
            query.setParameter("policyId", filter.getPolicyId());
            query.setParameter("policyVersionId", filter.getPolicyVersionId());
            query.setParameter("onlyActive", filter.isOnlyActive());
            query.setParameter("weightVector", WEIGHT_VECTOR);
            query.setParameter("weightFts", WEIGHT_FTS);
            query.setParameter("rrfK", RRF_K);
            query.setParameter("maxResults", filter.getMaxResults());

            List<?> results = query.getResultList();
            return mapSqlResultsToScoredChunks(results, false);
        } catch (Exception e) {
            log.warn("Native hybrid query failed ({}), falling back to in-memory search.", e.getMessage());
            return inMemoryFallbackSearch(queryVector, queryText, filter, true);
        }
    }

    private List<ScoredDocumentChunk> mapSqlResultsToScoredChunks(List<?> rows, boolean vectorOnly) {
        List<ScoredDocumentChunk> list = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            UUID chunkId = row[0] instanceof UUID ? (UUID) row[0] : UUID.fromString(row[0].toString());
            UUID policyVersionId = row[1] instanceof UUID ? (UUID) row[1] : UUID.fromString(row[1].toString());
            UUID policyId = row[2] instanceof UUID ? (UUID) row[2] : UUID.fromString(row[2].toString());
            String policyTitle = (String) row[3];
            int versionNum = ((Number) row[4]).intValue();
            int statusCode = ((Number) row[5]).intValue();
            PolicyVersionStatusEnum statusEnum = mapStatusCodeToEnum(statusCode);

            LocalDateTime effectiveFrom = row[6] instanceof Timestamp ? ((Timestamp) row[6]).toLocalDateTime() : null;
            LocalDateTime effectiveTo = row[7] instanceof Timestamp ? ((Timestamp) row[7]).toLocalDateTime() : null;

            String clauseRef = (String) row[8];
            String hierarchy = (String) row[9];
            String content = (String) row[10];
            double vectorScore = ((Number) row[11]).doubleValue();
            double ftsScore = ((Number) row[12]).doubleValue();

            double rrfScore = 0.0;
            double finalScore;
            if (vectorOnly) {
                finalScore = vectorScore;
            } else {
                rrfScore = ((Number) row[13]).doubleValue();
                // Normalized final relevance score combining vector score and FTS match bonus
                double combined = Math.max(vectorScore, ftsScore > 0 ? (vectorScore * 0.50 + 0.50) : vectorScore);
                if (ftsScore > 0 && vectorScore > 0.20) {
                    combined = Math.max(combined, 0.85);
                }
                finalScore = Math.min(1.0, Math.max(combined, vectorScore));
            }

            Map<String, Object> metadata = parseMetadataJson(row[vectorOnly ? 13 : 14]);
            boolean isExpired = statusEnum == PolicyVersionStatusEnum.EXPIRED ||
                    (effectiveTo != null && effectiveTo.isBefore(LocalDateTime.now()));

            list.add(ScoredDocumentChunk.builder()
                    .chunkId(chunkId)
                    .policyVersionId(policyVersionId)
                    .policyId(policyId)
                    .policyTitle(policyTitle)
                    .versionNumber(versionNum)
                    .policyStatus(statusEnum.name())
                    .effectiveFrom(effectiveFrom)
                    .effectiveTo(effectiveTo)
                    .clauseReference(clauseRef)
                    .sectionHierarchy(hierarchy)
                    .content(content)
                    .vectorScore(vectorScore)
                    .ftsScore(ftsScore)
                    .hybridRrfScore(rrfScore)
                    .finalRelevanceScore(finalScore)
                    .expired(isExpired)
                    .metadata(metadata)
                    .build());
        }
        return list;
    }

    private List<ScoredDocumentChunk> inMemoryFallbackSearch(float[] queryVector, String queryText, RetrievalFilter filter, boolean hybrid) {
        List<DocumentChunk> allChunks = documentChunkRepository.findAll();
        List<ScoredDocumentChunk> scored = new ArrayList<>();

        for (DocumentChunk chunk : allChunks) {
            PolicyVersion pv = chunk.getPolicyVersion();
            Policy p = pv != null ? pv.getPolicy() : null;

            if (filter.isOnlyActive() && (pv == null || pv.getStatus() != PolicyVersionStatusEnum.ACTIVE)) {
                continue;
            }
            if (filter.getPolicyId() != null && (p == null || !p.getId().equals(filter.getPolicyId()))) {
                continue;
            }
            if (filter.getPolicyVersionId() != null && (pv == null || !pv.getId().equals(filter.getPolicyVersionId()))) {
                continue;
            }

            float[] chunkVec = unboxFloatArray(chunk.getEmbedding());
            double cosine = computeCosineSimilarity(queryVector, chunkVec);
            double ftsScore = 0.0;

            if (queryText != null && !queryText.isBlank()) {
                ftsScore = computeKeywordScore(chunk.getContent(), queryText);
            }

            double combined = Math.max(cosine, ftsScore > 0 ? (cosine * 0.50 + 0.50) : cosine);
            if (ftsScore > 0 && cosine > 0.20) {
                combined = Math.max(combined, 0.85);
            }
            double finalScore = hybrid ? combined : cosine;
            boolean isExpired = pv != null && (pv.getStatus() == PolicyVersionStatusEnum.EXPIRED ||
                    (pv.getEffectiveTo() != null && pv.getEffectiveTo().isBefore(LocalDateTime.now())));

            scored.add(ScoredDocumentChunk.builder()
                    .chunkId(chunk.getId())
                    .policyVersionId(pv != null ? pv.getId() : null)
                    .policyId(p != null ? p.getId() : null)
                    .policyTitle(p != null ? p.getTitle() : "Regulatory Policy")
                    .versionNumber(pv != null ? pv.getVersion() : 1)
                    .policyStatus(pv != null ? pv.getStatus().name() : "ACTIVE")
                    .effectiveFrom(pv != null ? pv.getEffectiveFrom() : null)
                    .effectiveTo(pv != null ? pv.getEffectiveTo() : null)
                    .clauseReference(chunk.getClauseReference())
                    .sectionHierarchy(chunk.getSectionHierarchy())
                    .content(chunk.getContent())
                    .vectorScore(cosine)
                    .ftsScore(ftsScore)
                    .hybridRrfScore(finalScore)
                    .finalRelevanceScore(finalScore)
                    .expired(isExpired)
                    .metadata(chunk.getMetadata() != null ? chunk.getMetadata() : Map.of())
                    .build());
        }

        scored.sort((a, b) -> Double.compare(b.getFinalRelevanceScore(), a.getFinalRelevanceScore()));
        return scored.stream().limit(filter.getMaxResults()).toList();
    }

    private double computeCosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) return 0.0;
        double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "can", "cannot", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself",
            "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "me", "more", "most", "my", "myself",
            "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves",
            "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their",
            "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while", "who",
            "whom", "why", "with", "would", "you", "your", "yours", "yourself", "yourselves"
    );

    private double computeKeywordScore(String text, String query) {
        if (text == null || query == null) return 0.0;
        String lowerText = text.toLowerCase();
        String[] rawKeywords = query.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        int matches = 0;
        int meaningfulKeywords = 0;
        for (String kw : rawKeywords) {
            if (kw.length() > 2 && !STOP_WORDS.contains(kw)) {
                meaningfulKeywords++;
                if (lowerText.contains(kw)) {
                    matches++;
                }
            }
        }
        return meaningfulKeywords > 0 ? (double) matches / meaningfulKeywords : 0.0;
    }

    private String formatVectorToString(float[] vec) {
        if (vec == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            sb.append(vec[i]);
            if (i < vec.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] unboxFloatArray(Float[] boxed) {
        if (boxed == null) return null;
        float[] primitive = new float[boxed.length];
        for (int i = 0; i < boxed.length; i++) {
            primitive[i] = boxed[i] != null ? boxed[i] : 0.0f;
        }
        return primitive;
    }

    private PolicyVersionStatusEnum mapStatusCodeToEnum(int code) {
        return switch (code) {
            case 0 -> PolicyVersionStatusEnum.DRAFT;
            case 1 -> PolicyVersionStatusEnum.APPROVED;
            case 2 -> PolicyVersionStatusEnum.ACTIVE;
            default -> PolicyVersionStatusEnum.EXPIRED;
        };
    }

    private Map<String, Object> parseMetadataJson(Object jsonSource) {
        if (jsonSource == null) return Map.of();
        if (jsonSource instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) jsonSource;
            return map;
        }
        try {
            return objectMapper.readValue(jsonSource.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
