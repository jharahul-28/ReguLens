package com.rj.ReguLens.evaluation.service.impl;

import com.rj.ReguLens.entity.EvaluationBenchmarkSet;
import com.rj.ReguLens.entity.EvaluationRunResult;
import com.rj.ReguLens.evaluation.metrics.EvaluationMetricsCalculator;
import com.rj.ReguLens.evaluation.model.BenchmarkSummaryMetrics;
import com.rj.ReguLens.evaluation.model.QueryEvaluationResult;
import com.rj.ReguLens.evaluation.seed.BenchmarkSeedData;
import com.rj.ReguLens.evaluation.service.EvaluationService;
import com.rj.ReguLens.exception.ResourceNotFound;
import com.rj.ReguLens.generation.model.AttributedClaim;
import com.rj.ReguLens.generation.model.ComplianceAnswer;
import com.rj.ReguLens.generation.model.GroundednessStatus;
import com.rj.ReguLens.generation.service.ComplianceGenerationService;
import com.rj.ReguLens.repository.EvaluationBenchmarkSetRepository;
import com.rj.ReguLens.repository.EvaluationRunResultRepository;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class EvaluationServiceImpl implements EvaluationService {

    EvaluationBenchmarkSetRepository benchmarkSetRepository;
    EvaluationRunResultRepository runResultRepository;
    ComplianceGenerationService generationService;
    EvaluationMetricsCalculator metricsCalculator;

    @Transactional
    @Override
    public BenchmarkSummaryMetrics runEvaluation() {
        log.info("Starting automated compliance RAG evaluation benchmark run...");

        List<EvaluationBenchmarkSet> benchmarks = benchmarkSetRepository.findByActiveTrue();
        if (benchmarks.isEmpty()) {
            log.info("No active benchmark test cases found in DB. Seeding default golden benchmark set.");
            benchmarks = seedBenchmarkSet();
        }

        List<QueryEvaluationResult> queryResults = new ArrayList<>();

        for (EvaluationBenchmarkSet benchmarkCase : benchmarks) {
            String query = benchmarkCase.getQueryText();
            RetrievalFilter filter = RetrievalFilter.builder()
                    .minSimilarityScore(0.70)
                    .maxResults(5)
                    .build();

            ComplianceAnswer answer = generationService.answerComplianceQuery(query, filter);

            List<String> retrievedClauseRefs = new ArrayList<>();
            if (answer.getClaims() != null) {
                for (AttributedClaim claim : answer.getClaims()) {
                    if (claim.getClauseReference() != null) {
                        retrievedClauseRefs.add(claim.getClauseReference());
                    }
                }
            }

            boolean isFaithful = answer.getGroundednessStatus() == GroundednessStatus.VERIFIED_ENTAILED
                    || answer.getGroundednessStatus() == GroundednessStatus.THRESHOLD_REJECTED
                    || answer.getGroundednessStatus() == GroundednessStatus.MISSING_CONTEXT;

            QueryEvaluationResult evaluated = metricsCalculator.evaluateSingleQuery(
                    query,
                    benchmarkCase.getExpectedClauseReferences(),
                    retrievedClauseRefs,
                    answer.getAnswer(),
                    benchmarkCase.getGroundTruthAnswer(),
                    isFaithful,
                    answer.isHasExpiredPolicyNotice(),
                    !answer.isClearsConfidenceThreshold(),
                    answer.getRetrievalLatencyMs(),
                    answer.getGenerationLatencyMs(),
                    answer.getTotalLatencyMs()
            );

            queryResults.add(evaluated);
        }

        BenchmarkSummaryMetrics summary = metricsCalculator.aggregateResults(queryResults);

        // Persist evaluation run summary
        Map<String, Object> reportDetails = new HashMap<>();
        reportDetails.put("avg_answer_relevance_pct", summary.getAvgAnswerRelevance());
        reportDetails.put("policy_lifecycle_compliance_pct", summary.getPolicyLifecycleComplianceRate());
        reportDetails.put("avg_retrieval_latency_ms", summary.getAvgRetrievalLatencyMs());
        reportDetails.put("avg_generation_latency_ms", summary.getAvgGenerationLatencyMs());

        EvaluationRunResult runResult = EvaluationRunResult.builder()
                .totalQueries(summary.getTotalQueries())
                .hitRateAt5(BigDecimal.valueOf(summary.getHitRateAt5()))
                .meanReciprocalRank(BigDecimal.valueOf(summary.getMeanReciprocalRank()))
                .precisionAt3(BigDecimal.valueOf(summary.getPrecisionAt3()))
                .faithfulnessRate(BigDecimal.valueOf(summary.getFaithfulnessRate()))
                .avgTotalLatencyMs(summary.getAvgTotalLatencyMs())
                .benchmarkReport(reportDetails)
                .build();

        runResultRepository.save(runResult);

        log.info("Benchmark evaluation completed: {} queries evaluated | HitRate@5: {}% | MRR: {} | Faithfulness: {}% | Relevance: {}% | Avg Latency: {}ms",
                summary.getTotalQueries(),
                summary.getHitRateAt5(),
                summary.getMeanReciprocalRank(),
                summary.getFaithfulnessRate(),
                summary.getAvgAnswerRelevance(),
                summary.getAvgTotalLatencyMs());

        return summary;
    }

    @Transactional
    @Override
    public List<EvaluationBenchmarkSet> seedBenchmarkSet() {
        log.info("Seeding golden benchmark dataset...");
        List<EvaluationBenchmarkSet> cases = BenchmarkSeedData.getGoldenBenchmarkCases();
        return benchmarkSetRepository.saveAll(cases);
    }

    @Override
    public List<EvaluationRunResult> getEvaluationHistory() {
        return runResultRepository.findAllByOrderByRunTimestampDesc();
    }

    @Override
    public EvaluationRunResult getEvaluationRunById(UUID id) {
        return runResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Evaluation run result not found with id: " + id));
    }
}
