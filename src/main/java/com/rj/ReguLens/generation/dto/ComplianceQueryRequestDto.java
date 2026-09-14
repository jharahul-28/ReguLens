package com.rj.ReguLens.generation.dto;

import java.util.UUID;

public record ComplianceQueryRequestDto(
        String question,
        String jurisdiction,
        String regulationName,
        UUID policyId,
        Boolean onlyActive,
        Double minSimilarityScore,
        Integer maxContextChunks
) {}
