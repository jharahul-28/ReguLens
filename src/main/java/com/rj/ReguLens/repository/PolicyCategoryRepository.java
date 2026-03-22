package com.rj.ReguLens.repository;

import com.rj.ReguLens.entity.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PolicyCategoryRepository extends JpaRepository<PolicyCategory, UUID> {
    PolicyCategory findByName(String category);
}
