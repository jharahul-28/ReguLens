package com.rj.ReguLens.evaluation.service;

import com.rj.ReguLens.entity.EvaluationBenchmarkSet;
import com.rj.ReguLens.entity.EvaluationRunResult;
import com.rj.ReguLens.evaluation.metrics.EvaluationMetricsCalculator;
import com.rj.ReguLens.evaluation.model.BenchmarkSummaryMetrics;
import com.rj.ReguLens.evaluation.seed.BenchmarkSeedData;
import com.rj.ReguLens.evaluation.service.impl.EvaluationServiceImpl;
import com.rj.ReguLens.generation.model.AttributedClaim;
import com.rj.ReguLens.generation.model.ComplianceAnswer;
import com.rj.ReguLens.generation.model.GroundednessStatus;
import com.rj.ReguLens.generation.service.ComplianceGenerationService;
import com.rj.ReguLens.repository.EvaluationBenchmarkSetRepository;
import com.rj.ReguLens.repository.EvaluationRunResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationBenchmarkSetRepository benchmarkSetRepository;

    @Mock
    private EvaluationRunResultRepository runResultRepository;

    @Mock
    private ComplianceGenerationService generationService;

    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        EvaluationMetricsCalculator calculator = new EvaluationMetricsCalculator();
        evaluationService = new EvaluationServiceImpl(
                benchmarkSetRepository,
                runResultRepository,
                generationService,
                calculator
        );
    }

    @Test
    void shouldRunBenchmarkEvaluationSuccessfullyAcrossGoldenDataset() {
        List<EvaluationBenchmarkSet> testCases = BenchmarkSeedData.getGoldenBenchmarkCases();
        when(benchmarkSetRepository.findByActiveTrue()).thenReturn(testCases);

        Map<String, EvaluationBenchmarkSet> caseMap = testCases.stream()
                .collect(Collectors.toMap(EvaluationBenchmarkSet::getQueryText, c -> c));

        when(generationService.answerComplianceQuery(anyString(), any())).thenAnswer(inv -> {
            String query = inv.getArgument(0);
            EvaluationBenchmarkSet bench = caseMap.get(query);

            if (bench == null || bench.getExpectedClauseReferences().isEmpty()) {
                return ComplianceAnswer.builder()
                        .queryText(query)
                        .answer("The indexed regulatory policy documents do not contain sufficient context to answer this compliance question.")
                        .clearsConfidenceThreshold(false)
                        .groundednessStatus(GroundednessStatus.MISSING_CONTEXT)
                        .retrievalLatencyMs(15)
                        .generationLatencyMs(0)
                        .totalLatencyMs(15)
                        .build();
            }

            boolean isExpired = bench.getGroundTruthAnswer().contains("⚠️ Regulatory Notice:");
            List<AttributedClaim> claims = new ArrayList<>();
            for (String clause : bench.getExpectedClauseReferences()) {
                claims.add(AttributedClaim.builder()
                        .statement("Compliance requirement under " + clause)
                        .clauseReference(clause)
                        .verified(true)
                        .build());
            }

            return ComplianceAnswer.builder()
                    .queryText(query)
                    .answer(bench.getGroundTruthAnswer())
                    .hasActivePolicy(!isExpired)
                    .hasExpiredPolicyNotice(isExpired)
                    .clearsConfidenceThreshold(true)
                    .claims(claims)
                    .groundednessStatus(GroundednessStatus.VERIFIED_ENTAILED)
                    .retrievalLatencyMs(35)
                    .generationLatencyMs(250)
                    .totalLatencyMs(285)
                    .build();
        });

        when(runResultRepository.save(any(EvaluationRunResult.class))).thenAnswer(inv -> inv.getArgument(0));

        BenchmarkSummaryMetrics summary = evaluationService.runEvaluation();

        assertNotNull(summary);
        assertEquals(testCases.size(), summary.getTotalQueries());
        assertTrue(summary.getHitRateAt5() >= 85.0, "HitRate@5 should be >= 85%");
        assertTrue(summary.getMeanReciprocalRank() >= 0.85, "MRR should be >= 0.85");
        assertTrue(summary.getFaithfulnessRate() >= 85.0, "Faithfulness rate should be >= 85%");
        assertTrue(summary.getAvgAnswerRelevance() >= 85.0, "Relevance should be >= 85%");
        assertTrue(summary.getAvgTotalLatencyMs() < 30000, "Latency should be well under 30s target");
        verify(runResultRepository).save(any(EvaluationRunResult.class));
    }

    @Test
    void shouldSeedBenchmarkSet() {
        when(benchmarkSetRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<EvaluationBenchmarkSet> seeded = evaluationService.seedBenchmarkSet();

        assertNotNull(seeded);
        assertFalse(seeded.isEmpty());
        assertEquals(24, seeded.size());
        verify(benchmarkSetRepository).saveAll(anyList());
    }
}
