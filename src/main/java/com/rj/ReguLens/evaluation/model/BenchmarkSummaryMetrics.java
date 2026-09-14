package com.rj.ReguLens.evaluation.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BenchmarkSummaryMetrics {
    int totalQueries;
    double hitRateAt5;
    double meanReciprocalRank;
    double precisionAt3;
    double avgAnswerRelevance;
    double faithfulnessRate;
    double policyLifecycleComplianceRate;
    long avgRetrievalLatencyMs;
    long avgGenerationLatencyMs;
    long avgTotalLatencyMs;
    List<QueryEvaluationResult> queryResults;
}
