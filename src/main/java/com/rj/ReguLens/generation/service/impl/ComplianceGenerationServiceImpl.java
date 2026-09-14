package com.rj.ReguLens.generation.service.impl;

import com.rj.ReguLens.audit.model.AuditRecordCommand;
import com.rj.ReguLens.audit.service.AuditService;
import com.rj.ReguLens.generation.LlmClientPort;
import com.rj.ReguLens.generation.model.ComplianceAnswer;
import com.rj.ReguLens.generation.model.GroundednessStatus;
import com.rj.ReguLens.generation.model.LlmRequest;
import com.rj.ReguLens.generation.model.LlmResponse;
import com.rj.ReguLens.generation.prompt.RegulatoryPromptBuilder;
import com.rj.ReguLens.generation.service.ComplianceGenerationService;
import com.rj.ReguLens.generation.verifier.GroundednessVerifier;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import com.rj.ReguLens.retrieval.model.RetrievalResult;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import com.rj.ReguLens.retrieval.service.RetrievalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ComplianceGenerationServiceImpl implements ComplianceGenerationService {

    RetrievalService retrievalService;
    RegulatoryPromptBuilder promptBuilder;
    LlmClientPort llmClientPort;
    GroundednessVerifier groundednessVerifier;
    AuditService auditService;

    public static final String MISSING_CONTEXT_MESSAGE = 
            "The indexed regulatory policy documents do not contain sufficient context to answer this compliance question.";

    public static final String VERIFICATION_FAILED_MESSAGE = 
            "The generated compliance answer failed audit groundedness verification against the cited source clauses.";

    @Override
    public ComplianceAnswer answerComplianceQuery(String query, RetrievalFilter filter) {
        long overallStart = System.currentTimeMillis();
        log.info("Processing compliance question: '{}'", query);

        // 1. Retrieve Candidate Clauses
        RetrievalResult retrievalResult = retrievalService.retrieveRelevantClauses(query, filter);

        // 2. Similarity & Confidence Threshold Gating
        if (retrievalResult.isEmpty() || !retrievalResult.isClearsThreshold()) {
            log.info("Query '{}' did not clear retrieval threshold ({}), bypassing generation.",
                    query, filter.getMinSimilarityScore());

            long totalLatency = System.currentTimeMillis() - overallStart;
            GroundednessStatus status = retrievalResult.isEmpty() 
                    ? GroundednessStatus.MISSING_CONTEXT 
                    : GroundednessStatus.THRESHOLD_REJECTED;

            // Record audit trace for threshold-rejected query
            auditService.recordAuditEntry(AuditRecordCommand.builder()
                    .queryText(query)
                    .retrievedChunks(retrievalResult.getChunks())
                    .similarityThreshold(filter.getMinSimilarityScore())
                    .rawPrompt("N/A (Bypassed due to threshold gate)")
                    .generatedAnswer(MISSING_CONTEXT_MESSAGE)
                    .claims(List.of())
                    .groundednessStatus(status)
                    .modelIdentifier(llmClientPort.getModelIdentifier())
                    .promptTokens(0)
                    .completionTokens(0)
                    .retrievalLatencyMs(retrievalResult.getExecutionTimeMs())
                    .generationLatencyMs(0)
                    .totalLatencyMs(totalLatency)
                    .build());

            return ComplianceAnswer.builder()
                    .queryText(query)
                    .answer(MISSING_CONTEXT_MESSAGE)
                    .hasActivePolicy(false)
                    .hasExpiredPolicyNotice(false)
                    .clearsConfidenceThreshold(false)
                    .retrievalScore(retrievalResult.getMaxScore())
                    .claims(List.of())
                    .citedChunkIds(List.of())
                    .groundednessStatus(status)
                    .groundednessConfidence(1.0)
                    .retrievalLatencyMs(retrievalResult.getExecutionTimeMs())
                    .generationLatencyMs(0)
                    .totalLatencyMs(totalLatency)
                    .promptTokens(0)
                    .completionTokens(0)
                    .modelIdentifier(llmClientPort.getModelIdentifier())
                    .build();
        }

        List<ScoredDocumentChunk> chunks = retrievalResult.getChunks();
        boolean onlyExpired = retrievalResult.isOnlyExpiredFound();

        // 3. Build Regulatory Prompt
        LlmRequest llmRequest = promptBuilder.buildRequest(query, chunks, onlyExpired);

        // 4. Invoke LLM for Deterministic Grounded Generation
        long genStart = System.currentTimeMillis();
        LlmResponse llmResponse = llmClientPort.generate(llmRequest);
        long genLatency = System.currentTimeMillis() - genStart;

        String rawAnswer = llmResponse.getContent();

        // 5. Post-Generation Anti-Hallucination & Entailment Verification
        GroundednessVerifier.VerificationResult verification = groundednessVerifier.verify(rawAnswer, chunks);

        String finalAnswer = rawAnswer;
        if (verification.getStatus() == GroundednessStatus.VERIFICATION_FAILED) {
            log.warn("Generated answer failed entailment check (confidence: {}). Applying guardrail suppression.",
                    verification.getConfidence());
            finalAnswer = VERIFICATION_FAILED_MESSAGE;
        }

        long totalLatency = System.currentTimeMillis() - overallStart;

        // 6. Record Audit Trace
        auditService.recordAuditEntry(AuditRecordCommand.builder()
                .queryText(query)
                .retrievedChunks(chunks)
                .similarityThreshold(filter.getMinSimilarityScore())
                .rawPrompt(llmRequest.getUserPrompt())
                .generatedAnswer(finalAnswer)
                .claims(verification.getClaims())
                .groundednessStatus(verification.getStatus())
                .modelIdentifier(llmResponse.getModelName())
                .promptTokens(llmResponse.getPromptTokens())
                .completionTokens(llmResponse.getCompletionTokens())
                .retrievalLatencyMs(retrievalResult.getExecutionTimeMs())
                .generationLatencyMs(genLatency)
                .totalLatencyMs(totalLatency)
                .build());

        return ComplianceAnswer.builder()
                .queryText(query)
                .answer(finalAnswer)
                .hasActivePolicy(!onlyExpired)
                .hasExpiredPolicyNotice(onlyExpired)
                .clearsConfidenceThreshold(true)
                .retrievalScore(retrievalResult.getMaxScore())
                .claims(verification.getClaims())
                .citedChunkIds(verification.getCitedChunkIds())
                .groundednessStatus(verification.getStatus())
                .groundednessConfidence(verification.getConfidence())
                .retrievalLatencyMs(retrievalResult.getExecutionTimeMs())
                .generationLatencyMs(genLatency)
                .totalLatencyMs(totalLatency)
                .promptTokens(llmResponse.getPromptTokens())
                .completionTokens(llmResponse.getCompletionTokens())
                .modelIdentifier(llmResponse.getModelName())
                .build();
    }
}
