package com.rj.ReguLens.generation.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ComplianceAnswer {
    String queryText;
    String answer;
    boolean hasActivePolicy;
    boolean hasExpiredPolicyNotice;
    String expiredPolicyNotice;
    boolean clearsConfidenceThreshold;
    double retrievalScore;
    List<AttributedClaim> claims;
    List<UUID> citedChunkIds;
    GroundednessStatus groundednessStatus;
    double groundednessConfidence;
    long retrievalLatencyMs;
    long generationLatencyMs;
    long totalLatencyMs;
    int promptTokens;
    int completionTokens;
    String modelIdentifier;
}
