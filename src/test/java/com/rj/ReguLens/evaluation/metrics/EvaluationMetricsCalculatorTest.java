package com.rj.ReguLens.evaluation.metrics;

import com.rj.ReguLens.evaluation.model.BenchmarkSummaryMetrics;
import com.rj.ReguLens.evaluation.model.QueryEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationMetricsCalculatorTest {

    private EvaluationMetricsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new EvaluationMetricsCalculator();
    }

    @Test
    void shouldEvaluateSingleQueryHitAndRelevance() {
        String query = "Erasure rules under GDPR?";
        List<String> expected = List.of("Article 17(1)", "Article 17");
        List<String> retrieved = List.of("Article 17(1)", "Article 18");
        String generated = "Data subjects have the right to erasure of personal data.";
        String groundTruth = "Under GDPR Article 17, data subjects have the right to obtain erasure of personal data.";

        QueryEvaluationResult result = calculator.evaluateSingleQuery(
                query,
                expected,
                retrieved,
                generated,
                groundTruth,
                true,
                false,
                false,
                45,
                150,
                195
        );

        assertNotNull(result);
        assertTrue(result.isHitAt5());
        assertEquals(1.0, result.getReciprocalRank()); // Ranked #1
        assertTrue(result.getPrecisionAt3() > 0.0);
        assertTrue(result.getAnswerRelevanceScore() >= 0.60);
        assertTrue(result.isFaithful());
    }

    @Test
    void shouldAggregateSummaryMetricsCorrectly() {
        QueryEvaluationResult r1 = QueryEvaluationResult.builder()
                .hitAt5(true)
                .reciprocalRank(1.0)
                .precisionAt3(0.33)
                .answerRelevanceScore(0.90)
                .faithful(true)
                .lifecycleCompliant(true)
                .retrievalLatencyMs(50)
                .generationLatencyMs(200)
                .totalLatencyMs(250)
                .build();

        QueryEvaluationResult r2 = QueryEvaluationResult.builder()
                .hitAt5(true)
                .reciprocalRank(0.5) // Ranked #2
                .precisionAt3(0.33)
                .answerRelevanceScore(0.85)
                .faithful(true)
                .lifecycleCompliant(true)
                .retrievalLatencyMs(60)
                .generationLatencyMs(220)
                .totalLatencyMs(280)
                .build();

        BenchmarkSummaryMetrics summary = calculator.aggregateResults(List.of(r1, r2));

        assertNotNull(summary);
        assertEquals(2, summary.getTotalQueries());
        assertEquals(100.0, summary.getHitRateAt5());
        assertEquals(0.75, summary.getMeanReciprocalRank()); // (1.0 + 0.5) / 2
        assertEquals(100.0, summary.getFaithfulnessRate());
        assertEquals(87.5, summary.getAvgAnswerRelevance()); // (90 + 85) / 2
        assertEquals(265, summary.getAvgTotalLatencyMs()); // (250 + 280) / 2
    }
}
