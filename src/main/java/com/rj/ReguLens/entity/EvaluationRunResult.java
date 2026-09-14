package com.rj.ReguLens.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "evaluation_run_result")
@Builder
public class EvaluationRunResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @CreationTimestamp
    @Column(name = "run_timestamp", nullable = false, updatable = false)
    OffsetDateTime runTimestamp;

    @Column(name = "total_queries", nullable = false)
    Integer totalQueries;

    @Column(name = "hit_rate_at_5", nullable = false, precision = 5, scale = 2)
    BigDecimal hitRateAt5;

    @Column(name = "mean_reciprocal_rank", nullable = false, precision = 5, scale = 4)
    BigDecimal meanReciprocalRank;

    @Column(name = "precision_at_3", nullable = false, precision = 5, scale = 2)
    BigDecimal precisionAt3;

    @Column(name = "faithfulness_rate", nullable = false, precision = 5, scale = 2)
    BigDecimal faithfulnessRate;

    @Column(name = "avg_total_latency_ms", nullable = false)
    Long avgTotalLatencyMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benchmark_report", columnDefinition = "jsonb", nullable = false)
    Map<String, Object> benchmarkReport;
}
