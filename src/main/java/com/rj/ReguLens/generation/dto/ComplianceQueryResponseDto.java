package com.rj.ReguLens.generation.dto;

import java.util.List;
import java.util.UUID;

public record ComplianceQueryResponseDto(
        String question,
        String answer,
        boolean activePolicyUsed,
        boolean expiredPolicyNoticeIncluded,
        boolean clearsConfidenceThreshold,
        double retrievalScore,
        String groundednessStatus,
        double groundednessConfidence,
        List<AttributedClaimDto> claims,
        List<UUID> citedChunkIds,
        long totalLatencyMs,
        long retrievalLatencyMs,
        long generationLatencyMs,
        int promptTokens,
        int completionTokens,
        String modelName
) {}
