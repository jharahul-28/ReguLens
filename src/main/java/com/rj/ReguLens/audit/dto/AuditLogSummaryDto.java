package com.rj.ReguLens.audit.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogSummaryDto(
        UUID id,
        UUID userId,
        String queryText,
        String answerPreview,
        String groundednessStatus,
        int chunkCount,
        int citationCount,
        long totalLatencyMs,
        String modelIdentifier,
        OffsetDateTime createdAt
) {}
