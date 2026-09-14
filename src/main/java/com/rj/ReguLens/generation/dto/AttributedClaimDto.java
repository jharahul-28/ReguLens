package com.rj.ReguLens.generation.dto;

import java.util.UUID;

public record AttributedClaimDto(
        String statement,
        UUID citedChunkId,
        String clauseReference,
        boolean verified,
        String verificationReason
) {}
