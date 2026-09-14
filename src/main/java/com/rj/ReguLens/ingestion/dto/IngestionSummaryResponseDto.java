package com.rj.ReguLens.ingestion.dto;

import java.util.List;
import java.util.UUID;

public record IngestionSummaryResponseDto(
        UUID policyVersionId,
        int totalChunksCreated,
        int totalTokens,
        List<ChunkResponseDto> chunks
) {}
