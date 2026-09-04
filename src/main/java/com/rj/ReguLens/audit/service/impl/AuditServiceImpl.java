package com.rj.ReguLens.audit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rj.ReguLens.audit.dto.AuditLogDetailDto;
import com.rj.ReguLens.audit.dto.AuditLogSummaryDto;
import com.rj.ReguLens.audit.model.AuditLogFilter;
import com.rj.ReguLens.audit.model.AuditRecordCommand;
import com.rj.ReguLens.audit.service.AuditService;
import com.rj.ReguLens.entity.QueryAuditLog;
import com.rj.ReguLens.exception.ResourceNotFound;
import com.rj.ReguLens.generation.model.AttributedClaim;
import com.rj.ReguLens.repository.QueryAuditLogRepository;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuditServiceImpl implements AuditService {

    QueryAuditLogRepository auditLogRepository;
    ObjectMapper objectMapper;

    @Transactional
    @Override
    public QueryAuditLog recordAuditEntry(AuditRecordCommand command) {
        log.info("Recording query audit trace for query: '{}', status: {}",
                command.getQueryText(), command.getGroundednessStatus());

        List<Map<String, Object>> chunkMaps = new ArrayList<>();
        if (command.getRetrievedChunks() != null) {
            for (ScoredDocumentChunk c : command.getRetrievedChunks()) {
                Map<String, Object> map = new HashMap<>();
                map.put("chunk_id", c.getChunkId() != null ? c.getChunkId().toString() : null);
                map.put("policy_title", c.getPolicyTitle());
                map.put("version", c.getVersionNumber());
                map.put("policy_status", c.getPolicyStatus());
                map.put("clause_reference", c.getClauseReference());
                map.put("section_hierarchy", c.getSectionHierarchy());
                map.put("vector_score", c.getVectorScore());
                map.put("fts_score", c.getFtsScore());
                map.put("relevance_score", c.getFinalRelevanceScore());
                map.put("is_expired", c.isExpired());
                chunkMaps.add(map);
            }
        }

        List<Map<String, Object>> citationMaps = new ArrayList<>();
        if (command.getClaims() != null) {
            for (AttributedClaim claim : command.getClaims()) {
                Map<String, Object> map = new HashMap<>();
                map.put("statement", claim.getStatement());
                map.put("cited_chunk_id", claim.getCitedChunkId() != null ? claim.getCitedChunkId().toString() : null);
                map.put("clause_reference", claim.getClauseReference());
                map.put("verified", claim.isVerified());
                map.put("verification_reason", claim.getVerificationReason());
                citationMaps.add(map);
            }
        }

        QueryAuditLog logEntity = QueryAuditLog.builder()
                .userId(command.getUserId())
                .queryText(command.getQueryText())
                .retrievedChunks(chunkMaps)
                .similarityThreshold(BigDecimal.valueOf(command.getSimilarityThreshold()))
                .rawPrompt(command.getRawPrompt() != null ? command.getRawPrompt() : "")
                .generatedAnswer(command.getGeneratedAnswer() != null ? command.getGeneratedAnswer() : "")
                .structuredCitations(citationMaps)
                .groundednessStatus(command.getGroundednessStatus().name())
                .modelIdentifier(command.getModelIdentifier() != null ? command.getModelIdentifier() : "unknown")
                .modelVersion(command.getModelVersion() != null ? command.getModelVersion() : "1.0")
                .promptTokens(command.getPromptTokens())
                .completionTokens(command.getCompletionTokens())
                .retrievalLatencyMs(command.getRetrievalLatencyMs())
                .generationLatencyMs(command.getGenerationLatencyMs())
                .totalLatencyMs(command.getTotalLatencyMs())
                .build();

        return auditLogRepository.save(logEntity);
    }

    @Override
    public Page<AuditLogSummaryDto> getAuditLogs(AuditLogFilter filter, Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<QueryAuditLog> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (filter != null) {
                if (filter.getUserId() != null) {
                    predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
                }
                if (filter.getGroundednessStatus() != null && !filter.getGroundednessStatus().isBlank()) {
                    predicates.add(cb.equal(root.get("groundednessStatus"), filter.getGroundednessStatus()));
                }
                if (filter.getStartTime() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartTime()));
                }
                if (filter.getEndTime() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndTime()));
                }
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<QueryAuditLog> page = auditLogRepository.findAll(spec, pageable);

        return page.map(entity -> {
            String preview = entity.getGeneratedAnswer();
            if (preview != null && preview.length() > 120) {
                preview = preview.substring(0, 117) + "...";
            }
            int chunkCount = entity.getRetrievedChunks() != null ? entity.getRetrievedChunks().size() : 0;
            int citationCount = entity.getStructuredCitations() != null ? entity.getStructuredCitations().size() : 0;

            return new AuditLogSummaryDto(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getQueryText(),
                    preview,
                    entity.getGroundednessStatus(),
                    chunkCount,
                    citationCount,
                    entity.getTotalLatencyMs(),
                    entity.getModelIdentifier(),
                    entity.getCreatedAt()
            );
        });
    }

    @Override
    public AuditLogDetailDto getAuditLogById(UUID id) {
        QueryAuditLog entity = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Audit log not found with id: " + id));

        return new AuditLogDetailDto(
                entity.getId(),
                entity.getUserId(),
                entity.getQueryText(),
                entity.getRetrievedChunks(),
                entity.getSimilarityThreshold(),
                entity.getRawPrompt(),
                entity.getGeneratedAnswer(),
                entity.getStructuredCitations(),
                entity.getGroundednessStatus(),
                entity.getModelIdentifier(),
                entity.getModelVersion(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getRetrievalLatencyMs(),
                entity.getGenerationLatencyMs(),
                entity.getTotalLatencyMs(),
                entity.getCreatedAt()
        );
    }

    @Override
    public byte[] exportAuditReportCsv(OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime s = start != null ? start : OffsetDateTime.now().minusDays(30);
        OffsetDateTime e = end != null ? end : OffsetDateTime.now();

        List<QueryAuditLog> logs = auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(s, e);

        StringBuilder sb = new StringBuilder();
        sb.append("\"Log ID\",\"User ID\",\"Timestamp\",\"Query\",\"Groundedness Status\",\"Model\",\"Prompt Tokens\",\"Completion Tokens\",\"Latency (ms)\",\"Answer Preview\"\n");

        for (QueryAuditLog logEntry : logs) {
            String cleanQuery = escapeCsv(logEntry.getQueryText());
            String cleanAnswer = escapeCsv(logEntry.getGeneratedAnswer());
            if (cleanAnswer.length() > 150) {
                cleanAnswer = cleanAnswer.substring(0, 147) + "...";
            }

            sb.append("\"").append(logEntry.getId()).append("\",")
                    .append("\"").append(logEntry.getUserId() != null ? logEntry.getUserId() : "").append("\",")
                    .append("\"").append(logEntry.getCreatedAt()).append("\",")
                    .append("\"").append(cleanQuery).append("\",")
                    .append("\"").append(logEntry.getGroundednessStatus()).append("\",")
                    .append("\"").append(logEntry.getModelIdentifier()).append("\",")
                    .append(logEntry.getPromptTokens()).append(",")
                    .append(logEntry.getCompletionTokens()).append(",")
                    .append(logEntry.getTotalLatencyMs()).append(",")
                    .append("\"").append(cleanAnswer).append("\"\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportAuditReportJson(OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime s = start != null ? start : OffsetDateTime.now().minusDays(30);
        OffsetDateTime e = end != null ? end : OffsetDateTime.now();

        List<QueryAuditLog> logs = auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(s, e);
        List<AuditLogDetailDto> dtos = logs.stream().map(entity -> new AuditLogDetailDto(
                entity.getId(),
                entity.getUserId(),
                entity.getQueryText(),
                entity.getRetrievedChunks(),
                entity.getSimilarityThreshold(),
                entity.getRawPrompt(),
                entity.getGeneratedAnswer(),
                entity.getStructuredCitations(),
                entity.getGroundednessStatus(),
                entity.getModelIdentifier(),
                entity.getModelVersion(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getRetrievalLatencyMs(),
                entity.getGenerationLatencyMs(),
                entity.getTotalLatencyMs(),
                entity.getCreatedAt()
        )).toList();

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(dtos);
        } catch (Exception ex) {
            log.error("Failed to export audit report JSON", ex);
            throw new RuntimeException("Error exporting audit report: " + ex.getMessage(), ex);
        }
    }

    private String escapeCsv(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
    }
}
