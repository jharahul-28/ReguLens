package com.rj.ReguLens.retrieval.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record RetrievedChunkDto(
        UUID chunkId,
        UUID policyVersionId,
        UUID policyId,
        String policyTitle,
        int versionNumber,
        String policyStatus,
        boolean expired,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String clauseReference,
        String sectionHierarchy,
        String content,
        double relevanceScore,
        double vectorScore,
        double ftsScore,
        Map<String, Object> metadata
) {}
