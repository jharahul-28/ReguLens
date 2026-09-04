package com.rj.ReguLens.retrieval.service.impl;

import com.rj.ReguLens.embedding.service.EmbeddingPipelineService;
import com.rj.ReguLens.retrieval.RetrievalPort;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import com.rj.ReguLens.retrieval.model.RetrievalResult;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import com.rj.ReguLens.retrieval.service.RetrievalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RetrievalServiceImpl implements RetrievalService {

    EmbeddingPipelineService embeddingPipelineService;
    RetrievalPort retrievalPort;

    @Override
    public RetrievalResult retrieveRelevantClauses(String queryText, RetrievalFilter filter) {
        long startTime = System.currentTimeMillis();
        log.info("Executing compliance retrieval for query: '{}', filter: {}", queryText, filter);

        if (queryText == null || queryText.isBlank()) {
            return RetrievalResult.builder()
                    .queryText(queryText)
                    .chunks(List.of())
                    .maxScore(0.0)
                    .threshold(filter.getMinSimilarityScore())
                    .clearsThreshold(false)
                    .onlyExpiredFound(false)
                    .executionTimeMs(0)
                    .build();
        }

        // 1. Generate Query Embedding Vector
        float[] queryVector = embeddingPipelineService.generateEmbeddingForQuery(queryText);

        // 2. Execute Hybrid Retrieval (Vector Cosine + FTS + RRF)
        List<ScoredDocumentChunk> candidateChunks = retrievalPort.searchHybrid(queryVector, queryText, filter);

        // 3. Re-rank & Apply Policy Lifecycle Rules
        List<ScoredDocumentChunk> reRankedChunks = applyPolicyLifecycleReRanking(candidateChunks, filter);

        // 4. Calculate Max Score and Threshold Clearance
        double maxScore = reRankedChunks.stream()
                .mapToDouble(ScoredDocumentChunk::getFinalRelevanceScore)
                .max()
                .orElse(0.0);

        boolean clearsThreshold = maxScore >= filter.getMinSimilarityScore();

        // 5. Detect if only expired policies matched
        boolean onlyExpiredFound = !reRankedChunks.isEmpty() &&
                reRankedChunks.stream().allMatch(ScoredDocumentChunk::isExpired);

        long latency = System.currentTimeMillis() - startTime;
        log.info("Retrieval completed in {}ms: found {} chunks, maxScore={}, clearsThreshold={}, onlyExpired={}",
                latency, reRankedChunks.size(), maxScore, clearsThreshold, onlyExpiredFound);

        return RetrievalResult.builder()
                .queryText(queryText)
                .chunks(reRankedChunks)
                .maxScore(maxScore)
                .threshold(filter.getMinSimilarityScore())
                .clearsThreshold(clearsThreshold)
                .onlyExpiredFound(onlyExpiredFound)
                .executionTimeMs(latency)
                .build();
    }

    private List<ScoredDocumentChunk> applyPolicyLifecycleReRanking(List<ScoredDocumentChunk> candidates, RetrievalFilter filter) {
        if (candidates.isEmpty()) return List.of();

        List<ScoredDocumentChunk> mutable = new ArrayList<>(candidates);

        // Prioritize ACTIVE versions over EXPIRED versions
        mutable.sort((a, b) -> {
            // If one is active and the other is expired, active wins unless score is substantially lower
            if (!a.isExpired() && b.isExpired()) {
                if (a.getFinalRelevanceScore() >= b.getFinalRelevanceScore() * 0.85) {
                    return -1;
                }
            } else if (a.isExpired() && !b.isExpired()) {
                if (b.getFinalRelevanceScore() >= a.getFinalRelevanceScore() * 0.85) {
                    return 1;
                }
            }
            return Double.compare(b.getFinalRelevanceScore(), a.getFinalRelevanceScore());
        });

        return mutable.stream().limit(filter.getMaxResults()).toList();
    }
}
