package com.rj.ReguLens.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "policy_version")
@Builder
public class PolicyVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    @JoinColumn(name = "policy_id")
    Policy policy;

    Integer version;

    @Column(nullable = false)
    String content;

    @Column(nullable = false)
    PolicyVersionStatusEnum status;

    @Column(
            name = "content_tsv",
            columnDefinition = "tsvector",
            insertable = false,
            updatable = false
    )
    String contentTsv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    User approvedBy;

    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(updatable = false)
    LocalDateTime effectiveFrom;

    LocalDateTime effectiveTo;
}
