package com.rj.ReguLens.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "query_audit_log")
@Builder
public class QueryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id")
    UUID userId;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    String queryText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retrieved_chunks", columnDefinition = "jsonb", nullable = false)
    List<Map<String, Object>> retrievedChunks;

    @Column(name = "similarity_threshold", nullable = false, precision = 4, scale = 3)
    BigDecimal similarityThreshold;

    @Column(name = "raw_prompt", nullable = false, columnDefinition = "TEXT")
    String rawPrompt;

    @Column(name = "generated_answer", nullable = false, columnDefinition = "TEXT")
    String generatedAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structured_citations", columnDefinition = "jsonb", nullable = false)
    List<Map<String, Object>> structuredCitations;

    @Column(name = "groundedness_status", nullable = false, length = 50)
    String groundednessStatus;

    @Column(name = "model_identifier", nullable = false, length = 100)
    String modelIdentifier;

    @Column(name = "model_version", nullable = false, length = 50)
    String modelVersion;

    @Column(name = "prompt_tokens", nullable = false)
    Integer promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    Integer completionTokens;

    @Column(name = "retrieval_latency_ms", nullable = false)
    Long retrievalLatencyMs;

    @Column(name = "generation_latency_ms", nullable = false)
    Long generationLatencyMs;

    @Column(name = "total_latency_ms", nullable = false)
    Long totalLatencyMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;
}
