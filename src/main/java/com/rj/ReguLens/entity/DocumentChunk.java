package com.rj.ReguLens.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "document_chunk",
        uniqueConstraints = @UniqueConstraint(columnNames = {"policy_version_id", "chunk_index"})
)
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_version_id", nullable = false)
    PolicyVersion policyVersion;

    @Column(name = "chunk_index", nullable = false)
    Integer chunkIndex;

    @Column(name = "clause_reference", nullable = false)
    String clauseReference;

    @Column(name = "section_hierarchy", nullable = false, columnDefinition = "TEXT")
    String sectionHierarchy;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "token_count", nullable = false)
    Integer tokenCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    Map<String, Object> metadata;

    @Column(
            name = "content_tsv",
            columnDefinition = "tsvector",
            insertable = false,
            updatable = false
    )
    String contentTsv;

    @Column(name = "embedding", columnDefinition = "vector(768)")
    Float[] embedding;

    @CreationTimestamp
    @Column(name = "created_at")
    OffsetDateTime createdAt;
}
