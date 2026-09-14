package com.rj.ReguLens.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "evaluation_benchmark_set")
@Builder
public class EvaluationBenchmarkSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    String queryText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_clause_references", columnDefinition = "jsonb", nullable = false)
    List<String> expectedClauseReferences;

    @Column(name = "ground_truth_answer", nullable = false, columnDefinition = "TEXT")
    String groundTruthAnswer;

    @Column(name = "jurisdiction", nullable = false, length = 50)
    String jurisdiction;

    @Builder.Default
    @Column(name = "active")
    Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;
}
