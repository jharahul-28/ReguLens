package com.rj.ReguLens.repository;

import com.rj.ReguLens.entity.EvaluationRunResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationRunResultRepository extends JpaRepository<EvaluationRunResult, UUID> {

    List<EvaluationRunResult> findAllByOrderByRunTimestampDesc();
}
