package com.rj.ReguLens.evaluation.controller;

import com.rj.ReguLens.entity.EvaluationBenchmarkSet;
import com.rj.ReguLens.entity.EvaluationRunResult;
import com.rj.ReguLens.evaluation.model.BenchmarkSummaryMetrics;
import com.rj.ReguLens.evaluation.service.EvaluationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/evaluation")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EvaluationController {

    EvaluationService evaluationService;

    @PostMapping("/run")
    public ResponseEntity<BenchmarkSummaryMetrics> runEvaluation() {
        log.info("Received request to trigger compliance benchmark evaluation run.");
        BenchmarkSummaryMetrics report = evaluationService.runEvaluation();
        return ResponseEntity.ok(report);
    }

    @PostMapping("/benchmark-set/seed")
    public ResponseEntity<List<EvaluationBenchmarkSet>> seedBenchmarkCases() {
        log.info("Received request to seed golden benchmark dataset.");
        List<EvaluationBenchmarkSet> seeded = evaluationService.seedBenchmarkSet();
        return ResponseEntity.ok(seeded);
    }

    @GetMapping("/history")
    public ResponseEntity<List<EvaluationRunResult>> getEvaluationHistory() {
        return ResponseEntity.ok(evaluationService.getEvaluationHistory());
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<EvaluationRunResult> getEvaluationRunById(@PathVariable UUID id) {
        return ResponseEntity.ok(evaluationService.getEvaluationRunById(id));
    }
}
