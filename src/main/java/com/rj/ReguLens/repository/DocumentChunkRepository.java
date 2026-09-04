package com.rj.ReguLens.repository;

import com.rj.ReguLens.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByPolicyVersionIdOrderByChunkIndexAsc(UUID policyVersionId);

    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE dc.policyVersion.id = :policyVersionId")
    void deleteByPolicyVersionId(@Param("policyVersionId") UUID policyVersionId);

    long countByPolicyVersionId(UUID policyVersionId);
}
