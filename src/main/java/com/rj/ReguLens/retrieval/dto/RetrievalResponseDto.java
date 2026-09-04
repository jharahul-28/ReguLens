package com.rj.ReguLens.retrieval.dto;

import java.util.List;

public record RetrievalResponseDto(
        String query,
        int totalResults,
        double maxSimilarityScore,
        double confidenceThreshold,
        boolean clearsConfidenceThreshold,
        boolean onlyExpiredPoliciesFound,
        long latencyMs,
        List<RetrievedChunkDto> chunks
) {}
