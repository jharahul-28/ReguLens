package com.rj.ReguLens.repository;

import com.rj.ReguLens.entity.PolicyVersion;
import com.rj.ReguLens.entity.PolicyVersionStatusEnum;
import com.rj.ReguLens.mapper.PolicyMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, UUID> {
    Set<PolicyVersion> findByPolicyIdAndStatus(UUID policyId, PolicyVersionStatusEnum policyVersionStatusEnum);

    Optional<PolicyVersion> findTopByPolicyIdOrderByVersionDesc(UUID policyId);
}
