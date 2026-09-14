package com.rj.ReguLens.retrieval.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class RetrievalFilter {
    String jurisdiction;
    String regulationName;
    UUID policyId;
    UUID policyVersionId;
    @Builder.Default
    boolean onlyActive = false;
    @Builder.Default
    boolean includeExpired = true;
    LocalDate asOfDate;
    @Builder.Default
    double minSimilarityScore = 0.70;
    @Builder.Default
    int maxResults = 5;
}
