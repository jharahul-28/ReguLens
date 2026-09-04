package com.rj.ReguLens.ingestion.dto;

import java.util.Map;
import java.util.UUID;

public record ChunkResponseDto(
        UUID id,
        int chunkIndex,
        String clauseReference,
        String sectionHierarchy,
        String content,
        int tokenCount,
        Map<String, Object> metadata
) {}
