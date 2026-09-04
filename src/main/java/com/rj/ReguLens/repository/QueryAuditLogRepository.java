package com.rj.ReguLens.repository;

import com.rj.ReguLens.entity.QueryAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QueryAuditLogRepository extends JpaRepository<QueryAuditLog, UUID>, JpaSpecificationExecutor<QueryAuditLog> {

    Page<QueryAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<QueryAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<QueryAuditLog> findByGroundednessStatusOrderByCreatedAtDesc(String groundednessStatus, Pageable pageable);

    List<QueryAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime start, OffsetDateTime end);
}
