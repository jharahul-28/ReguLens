package com.rj.ReguLens.evaluation.service;

import com.rj.ReguLens.entity.EvaluationBenchmarkSet;
import com.rj.ReguLens.entity.EvaluationRunResult;
import com.rj.ReguLens.evaluation.model.BenchmarkSummaryMetrics;

import java.util.List;
import java.util.UUID;

public interface EvaluationService {

    BenchmarkSummaryMetrics runEvaluation();

    List<EvaluationBenchmarkSet> seedBenchmarkSet();

    List<EvaluationRunResult> getEvaluationHistory();

    EvaluationRunResult getEvaluationRunById(UUID id);
}
