package com.rj.ReguLens.retrieval.service;

import com.rj.ReguLens.embedding.service.EmbeddingPipelineService;
import com.rj.ReguLens.retrieval.RetrievalPort;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import com.rj.ReguLens.retrieval.model.RetrievalResult;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import com.rj.ReguLens.retrieval.service.impl.RetrievalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private EmbeddingPipelineService embeddingPipelineService;

    @Mock
    private RetrievalPort retrievalPort;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new RetrievalServiceImpl(embeddingPipelineService, retrievalPort);
    }

    @Test
    void shouldRetrieveAndRankActiveOverExpiredChunks() {
        String query = "Right to erasure rules";
        float[] queryVec = new float[768];

        when(embeddingPipelineService.generateEmbeddingForQuery(query)).thenReturn(queryVec);

        ScoredDocumentChunk activeChunk = ScoredDocumentChunk.builder()
                .chunkId(UUID.randomUUID())
                .policyTitle("GDPR Standard")
                .versionNumber(2)
                .policyStatus("ACTIVE")
                .expired(false)
                .clauseReference("Article 17(1)")
                .content("Data subject erasure right...")
                .finalRelevanceScore(0.88)
                .build();

        ScoredDocumentChunk expiredChunk = ScoredDocumentChunk.builder()
                .chunkId(UUID.randomUUID())
                .policyTitle("GDPR Standard")
                .versionNumber(1)
                .policyStatus("EXPIRED")
                .expired(true)
                .clauseReference("Article 17(1)")
                .content("Old data subject erasure right...")
                .finalRelevanceScore(0.89) // slightly higher score, but active should win or be prioritized
                .build();

        RetrievalFilter filter = RetrievalFilter.builder().minSimilarityScore(0.70).build();

        when(retrievalPort.searchHybrid(eq(queryVec), eq(query), any())).thenReturn(List.of(expiredChunk, activeChunk));

        RetrievalResult result = retrievalService.retrieveRelevantClauses(query, filter);

        assertNotNull(result);
        assertEquals(2, result.getChunks().size());
        assertTrue(result.isClearsThreshold());
        assertFalse(result.isOnlyExpiredFound());

        // First result should be activeChunk due to lifecycle re-ranking
        assertEquals("ACTIVE", result.getChunks().get(0).getPolicyStatus());
    }

    @Test
    void shouldIdentifyWhenOnlyExpiredPoliciesMatch() {
        String query = "Discontinued compliance procedure";
        float[] queryVec = new float[768];

        when(embeddingPipelineService.generateEmbeddingForQuery(query)).thenReturn(queryVec);

        ScoredDocumentChunk expiredChunk = ScoredDocumentChunk.builder()
                .chunkId(UUID.randomUUID())
                .policyTitle("Legacy Security Policy")
                .versionNumber(1)
                .policyStatus("EXPIRED")
                .expired(true)
                .clauseReference("Section 9")
                .content("Old security standard...")
                .finalRelevanceScore(0.78)
                .build();

        RetrievalFilter filter = RetrievalFilter.builder().minSimilarityScore(0.70).build();

        when(retrievalPort.searchHybrid(eq(queryVec), eq(query), any())).thenReturn(List.of(expiredChunk));

        RetrievalResult result = retrievalService.retrieveRelevantClauses(query, filter);

        assertTrue(result.isClearsThreshold());
        assertTrue(result.isOnlyExpiredFound(), "Should flag that only expired policies matched");
    }

    @Test
    void shouldRejectWhenScoreBelowThreshold() {
        String query = "Completely unrelated query about baking cakes";
        float[] queryVec = new float[768];

        when(embeddingPipelineService.generateEmbeddingForQuery(query)).thenReturn(queryVec);

        ScoredDocumentChunk weakChunk = ScoredDocumentChunk.builder()
                .chunkId(UUID.randomUUID())
                .policyTitle("GDPR Standard")
                .finalRelevanceScore(0.42) // Below 0.70 threshold
                .build();

        RetrievalFilter filter = RetrievalFilter.builder().minSimilarityScore(0.70).build();

        when(retrievalPort.searchHybrid(eq(queryVec), eq(query), any())).thenReturn(List.of(weakChunk));

        RetrievalResult result = retrievalService.retrieveRelevantClauses(query, filter);

        assertFalse(result.isClearsThreshold(), "Expected clearsThreshold to be false when score < 0.70");
    }
}
