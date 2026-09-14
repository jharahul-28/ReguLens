package com.rj.ReguLens.ingestion.dto;

import java.time.LocalDate;

public record ChunkPreviewRequestDto(
        String title,
        String content,
        String jurisdiction,
        String regulationName,
        String versionTag,
        LocalDate effectiveDate
) {}
