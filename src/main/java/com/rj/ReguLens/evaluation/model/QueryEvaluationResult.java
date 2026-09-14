package com.rj.ReguLens.evaluation.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QueryEvaluationResult {
    String queryText;
    List<String> expectedClauses;
    List<String> retrievedClauses;
    String generatedAnswer;
    String groundTruthAnswer;
    boolean hitAt5;
    double reciprocalRank;
    double precisionAt3;
    double answerRelevanceScore;
    boolean faithful;
    boolean lifecycleCompliant;
    long retrievalLatencyMs;
    long generationLatencyMs;
    long totalLatencyMs;
}
