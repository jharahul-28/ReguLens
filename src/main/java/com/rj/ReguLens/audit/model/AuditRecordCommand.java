package com.rj.ReguLens.audit.model;

import com.rj.ReguLens.generation.model.AttributedClaim;
import com.rj.ReguLens.generation.model.GroundednessStatus;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class AuditRecordCommand {
    UUID userId;
    String queryText;
    List<ScoredDocumentChunk> retrievedChunks;
    double similarityThreshold;
    String rawPrompt;
    String generatedAnswer;
    List<AttributedClaim> claims;
    GroundednessStatus groundednessStatus;
    String modelIdentifier;
    @Builder.Default
    String modelVersion = "1.0";
    int promptTokens;
    int completionTokens;
    long retrievalLatencyMs;
    long generationLatencyMs;
    long totalLatencyMs;
}
