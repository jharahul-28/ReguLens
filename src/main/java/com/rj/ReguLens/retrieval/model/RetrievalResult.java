package com.rj.ReguLens.retrieval.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RetrievalResult {
    String queryText;
    List<ScoredDocumentChunk> chunks;
    double maxScore;
    double threshold;
    boolean clearsThreshold;
    boolean onlyExpiredFound;
    long executionTimeMs;

    public boolean isEmpty() {
        return chunks == null || chunks.isEmpty();
    }
}
