package com.rj.ReguLens.audit.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AuditLogDetailDto(
        UUID id,
        UUID userId,
        String queryText,
        List<Map<String, Object>> retrievedChunks,
        BigDecimal similarityThreshold,
        String rawPrompt,
        String generatedAnswer,
        List<Map<String, Object>> structuredCitations,
        String groundednessStatus,
        String modelIdentifier,
        String modelVersion,
        int promptTokens,
        int completionTokens,
        long retrievalLatencyMs,
        long generationLatencyMs,
        long totalLatencyMs,
        OffsetDateTime createdAt
) {}
