package com.rj.ReguLens.retrieval.dto;

import java.util.UUID;

public record RetrievalRequestDto(
        String query,
        String jurisdiction,
        String regulationName,
        UUID policyId,
        UUID policyVersionId,
        Boolean onlyActive,
        Double minSimilarityScore,
        Integer maxResults
) {}
