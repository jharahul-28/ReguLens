package com.rj.ReguLens.retrieval.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class ScoredDocumentChunk {
    UUID chunkId;
    UUID policyVersionId;
    UUID policyId;
    String policyTitle;
    int versionNumber;
    String policyStatus; // DRAFT, APPROVED, ACTIVE, EXPIRED
    LocalDateTime effectiveFrom;
    LocalDateTime effectiveTo;
    String clauseReference;
    String sectionHierarchy;
    String content;
    double vectorScore;
    double ftsScore;
    double hybridRrfScore;
    double finalRelevanceScore;
    boolean expired;
    Map<String, Object> metadata;

    public boolean isClearingThreshold(double threshold) {
        return finalRelevanceScore >= threshold;
    }
}
