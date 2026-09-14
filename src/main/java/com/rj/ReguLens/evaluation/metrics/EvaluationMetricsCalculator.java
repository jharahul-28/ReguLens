package com.rj.ReguLens.evaluation.metrics;

import com.rj.ReguLens.evaluation.model.BenchmarkSummaryMetrics;
import com.rj.ReguLens.evaluation.model.QueryEvaluationResult;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EvaluationMetricsCalculator {

    public QueryEvaluationResult evaluateSingleQuery(
            String queryText,
            List<String> expectedClauses,
            List<String> retrievedClauses,
            String generatedAnswer,
            String groundTruthAnswer,
            boolean isFaithful,
            boolean hasExpiredNotice,
            boolean isBelowThreshold,
            long retrievalLatency,
            long generationLatency,
            long totalLatency
    ) {
        boolean expectedEmpty = expectedClauses == null || expectedClauses.isEmpty();

        // 1. Hit Rate@5
        boolean hitAt5;
        if (expectedEmpty) {
            hitAt5 = isBelowThreshold || (retrievedClauses == null || retrievedClauses.isEmpty());
        } else {
            hitAt5 = containsAnyExpected(retrievedClauses, expectedClauses, 5);
        }

        // 2. Reciprocal Rank (MRR)
        double mrr = 0.0;
        if (expectedEmpty) {
            mrr = hitAt5 ? 1.0 : 0.0;
        } else if (retrievedClauses != null) {
            for (int i = 0; i < retrievedClauses.size(); i++) {
                if (matchesAnyExpected(retrievedClauses.get(i), expectedClauses)) {
                    mrr = 1.0 / (i + 1);
                    break;
                }
            }
        }

        // 3. Precision@3
        double precisionAt3 = 0.0;
        if (expectedEmpty) {
            precisionAt3 = hitAt5 ? 1.0 : 0.0;
        } else if (retrievedClauses != null && !retrievedClauses.isEmpty()) {
            int top3Limit = Math.min(3, retrievedClauses.size());
            int matched = 0;
            for (int i = 0; i < top3Limit; i++) {
                if (matchesAnyExpected(retrievedClauses.get(i), expectedClauses)) {
                    matched++;
                }
            }
            precisionAt3 = (double) matched / 3.0;
        }

        // 4. Answer Relevance Score
        double relevanceScore = calculateAnswerRelevance(generatedAnswer, groundTruthAnswer, isBelowThreshold, expectedEmpty);

        // 5. Policy Lifecycle Compliance Check
        boolean lifecycleCompliant;
        if (groundTruthAnswer != null && groundTruthAnswer.contains("⚠️ Regulatory Notice:")) {
            lifecycleCompliant = hasExpiredNotice || (generatedAnswer != null && generatedAnswer.contains("⚠️ Regulatory Notice:"));
        } else if (expectedEmpty) {
            lifecycleCompliant = isBelowThreshold || (generatedAnswer != null && generatedAnswer.contains("do not contain sufficient context"));
        } else {
            lifecycleCompliant = true;
        }

        return QueryEvaluationResult.builder()
                .queryText(queryText)
                .expectedClauses(expectedClauses != null ? expectedClauses : List.of())
                .retrievedClauses(retrievedClauses != null ? retrievedClauses : List.of())
                .generatedAnswer(generatedAnswer)
                .groundTruthAnswer(groundTruthAnswer)
                .hitAt5(hitAt5)
                .reciprocalRank(mrr)
                .precisionAt3(precisionAt3)
                .answerRelevanceScore(relevanceScore)
                .faithful(isFaithful)
                .lifecycleCompliant(lifecycleCompliant)
                .retrievalLatencyMs(retrievalLatency)
                .generationLatencyMs(generationLatency)
                .totalLatencyMs(totalLatency)
                .build();
    }

    public BenchmarkSummaryMetrics aggregateResults(List<QueryEvaluationResult> results) {
        if (results == null || results.isEmpty()) {
            return BenchmarkSummaryMetrics.builder()
                    .totalQueries(0)
                    .hitRateAt5(0.0)
                    .meanReciprocalRank(0.0)
                    .precisionAt3(0.0)
                    .avgAnswerRelevance(0.0)
                    .faithfulnessRate(0.0)
                    .policyLifecycleComplianceRate(0.0)
                    .avgRetrievalLatencyMs(0)
                    .avgGenerationLatencyMs(0)
                    .avgTotalLatencyMs(0)
                    .queryResults(List.of())
                    .build();
        }

        int n = results.size();
        long hitsAt5 = results.stream().filter(QueryEvaluationResult::isHitAt5).count();
        double sumMrr = results.stream().mapToDouble(QueryEvaluationResult::getReciprocalRank).sum();
        double sumP3 = results.stream().mapToDouble(QueryEvaluationResult::getPrecisionAt3).sum();
        double sumRel = results.stream().mapToDouble(QueryEvaluationResult::getAnswerRelevanceScore).sum();
        long faithfulCount = results.stream().filter(QueryEvaluationResult::isFaithful).count();
        long lifecycleCount = results.stream().filter(QueryEvaluationResult::isLifecycleCompliant).count();

        long sumRetrievalLat = results.stream().mapToLong(QueryEvaluationResult::getRetrievalLatencyMs).sum();
        long sumGenLat = results.stream().mapToLong(QueryEvaluationResult::getGenerationLatencyMs).sum();
        long sumTotalLat = results.stream().mapToLong(QueryEvaluationResult::getTotalLatencyMs).sum();

        return BenchmarkSummaryMetrics.builder()
                .totalQueries(n)
                .hitRateAt5(round2((double) hitsAt5 / n * 100.0))
                .meanReciprocalRank(round4(sumMrr / n))
                .precisionAt3(round2(sumP3 / n * 100.0))
                .avgAnswerRelevance(round2(sumRel / n * 100.0))
                .faithfulnessRate(round2((double) faithfulCount / n * 100.0))
                .policyLifecycleComplianceRate(round2((double) lifecycleCount / n * 100.0))
                .avgRetrievalLatencyMs(sumRetrievalLat / n)
                .avgGenerationLatencyMs(sumGenLat / n)
                .avgTotalLatencyMs(sumTotalLat / n)
                .queryResults(results)
                .build();
    }

    private boolean containsAnyExpected(List<String> retrieved, List<String> expected, int topK) {
        if (retrieved == null || expected == null) return false;
        int limit = Math.min(topK, retrieved.size());
        for (int i = 0; i < limit; i++) {
            if (matchesAnyExpected(retrieved.get(i), expected)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAnyExpected(String retrievedClause, List<String> expected) {
        if (retrievedClause == null || expected == null) return false;
        String cleanRetrieved = retrievedClause.toLowerCase().replaceAll("[^a-z0-9]", "");
        for (String exp : expected) {
            String cleanExp = exp.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (cleanRetrieved.contains(cleanExp) || cleanExp.contains(cleanRetrieved)) {
                return true;
            }
        }
        return false;
    }

    private double calculateAnswerRelevance(String generated, String groundTruth, boolean isBelowThreshold, boolean expectedEmpty) {
        if (expectedEmpty && isBelowThreshold) {
            return 1.0;
        }
        if (generated == null || groundTruth == null) return 0.0;

        Set<String> genTokens = tokenize(generated);
        Set<String> gtTokens = tokenize(groundTruth);

        if (gtTokens.isEmpty()) return 1.0;

        Set<String> intersection = new HashSet<>(genTokens);
        intersection.retainAll(gtTokens);

        // Primary recall against ground truth essential terminology
        double recall = (double) intersection.size() / gtTokens.size();
        return Math.min(1.0, recall);
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String w : words) {
            if (w.length() > 2 && !isStopWord(w)) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    private boolean isStopWord(String w) {
        return Set.of("the", "and", "under", "for", "with", "that", "this", "from", "are", "have").contains(w);
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private double round4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
