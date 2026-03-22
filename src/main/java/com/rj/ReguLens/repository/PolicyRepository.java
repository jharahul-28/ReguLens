package com.rj.ReguLens.repository;

import com.rj.ReguLens.entity.Policy;
import com.rj.ReguLens.entity.PolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {
    public Boolean existsByTitle(String title);
}
