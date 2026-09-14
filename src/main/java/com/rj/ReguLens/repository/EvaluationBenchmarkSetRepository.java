package com.rj.ReguLens.repository;

import com.rj.ReguLens.entity.EvaluationBenchmarkSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationBenchmarkSetRepository extends JpaRepository<EvaluationBenchmarkSet, UUID> {

    List<EvaluationBenchmarkSet> findByActiveTrue();

    List<EvaluationBenchmarkSet> findByJurisdictionAndActiveTrue(String jurisdiction);
}
