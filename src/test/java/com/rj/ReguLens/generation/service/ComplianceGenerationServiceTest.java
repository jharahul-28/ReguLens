package com.rj.ReguLens.generation.service;

import com.rj.ReguLens.audit.service.AuditService;
import com.rj.ReguLens.generation.adapter.MockChatAdapter;
import com.rj.ReguLens.generation.model.ComplianceAnswer;
import com.rj.ReguLens.generation.model.GroundednessStatus;
import com.rj.ReguLens.generation.prompt.RegulatoryPromptBuilder;
import com.rj.ReguLens.generation.service.impl.ComplianceGenerationServiceImpl;
import com.rj.ReguLens.generation.verifier.GroundednessVerifier;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import com.rj.ReguLens.retrieval.model.RetrievalResult;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import com.rj.ReguLens.retrieval.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceGenerationServiceTest {

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private AuditService auditService;

    private ComplianceGenerationService generationService;

    @BeforeEach
    void setUp() {
        RegulatoryPromptBuilder promptBuilder = new RegulatoryPromptBuilder();
        MockChatAdapter mockChat = new MockChatAdapter();
        GroundednessVerifier verifier = new GroundednessVerifier();

        generationService = new ComplianceGenerationServiceImpl(
                retrievalService,
                promptBuilder,
                mockChat,
                verifier,
                auditService
        );
    }

    @Test
    void shouldGenerateGroundedAnswerWhenThresholdClears() {
        UUID chunkId = UUID.randomUUID();
        ScoredDocumentChunk chunk = ScoredDocumentChunk.builder()
                .chunkId(chunkId)
                .policyTitle("GDPR Standard")
                .versionNumber(1)
                .policyStatus("ACTIVE")
                .clauseReference("Article 17")
                .content("Organizations must implement data retention protocols and verify access controls.")
                .finalRelevanceScore(0.85)
                .build();

        RetrievalResult retrievalResult = RetrievalResult.builder()
                .queryText("What are the audit protocols?")
                .chunks(List.of(chunk))
                .maxScore(0.85)
                .threshold(0.70)
                .clearsThreshold(true)
                .onlyExpiredFound(false)
                .executionTimeMs(120)
                .build();

        RetrievalFilter filter = RetrievalFilter.builder().minSimilarityScore(0.70).build();

        when(retrievalService.retrieveRelevantClauses(eq("What are the audit protocols?"), any()))
                .thenReturn(retrievalResult);

        ComplianceAnswer answer = generationService.answerComplianceQuery("What are the audit protocols?", filter);

        assertNotNull(answer);
        assertTrue(answer.isClearsConfidenceThreshold());
        assertTrue(answer.isHasActivePolicy());
        assertFalse(answer.isHasExpiredPolicyNotice());
        assertEquals(GroundednessStatus.VERIFIED_ENTAILED, answer.getGroundednessStatus());
        assertFalse(answer.getClaims().isEmpty());
        verify(auditService).recordAuditEntry(any());
    }

    @Test
    void shouldReturnMissingContextMessageWhenBelowThreshold() {
        RetrievalResult weakResult = RetrievalResult.builder()
                .queryText("Unrelated query")
                .chunks(List.of())
                .maxScore(0.35)
                .threshold(0.70)
                .clearsThreshold(false)
                .executionTimeMs(50)
                .build();

        RetrievalFilter filter = RetrievalFilter.builder().minSimilarityScore(0.70).build();

        when(retrievalService.retrieveRelevantClauses(eq("Unrelated query"), any()))
                .thenReturn(weakResult);

        ComplianceAnswer answer = generationService.answerComplianceQuery("Unrelated query", filter);

        assertEquals(ComplianceGenerationServiceImpl.MISSING_CONTEXT_MESSAGE, answer.getAnswer());
        assertFalse(answer.isClearsConfidenceThreshold());
        assertEquals(GroundednessStatus.MISSING_CONTEXT, answer.getGroundednessStatus());
        verify(auditService).recordAuditEntry(any());
    }
}
