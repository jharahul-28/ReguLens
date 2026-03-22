package com.rj.ReguLens.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "query_audit")
public class QueryAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    String question;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "query_audit_policy_versions",
            joinColumns = @JoinColumn(name = "query_audit_id"),
            inverseJoinColumns = @JoinColumn(name = "policy_version_id")
    )
    Set<PolicyVersion> retrievedPolicyVersions = new HashSet<>();

    @Column(nullable = false)
    String aiResponse;

    @CreationTimestamp
    LocalDateTime createdAt;
}
