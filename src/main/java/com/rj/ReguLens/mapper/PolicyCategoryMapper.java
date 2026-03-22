package com.rj.ReguLens.mapper;

import com.rj.ReguLens.dto.policy.PolicyCategoryCreationResponseDto;
import com.rj.ReguLens.entity.PolicyCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public interface PolicyCategoryMapper {
    PolicyCategoryCreationResponseDto policyCategoryToPolicyCategoryResponseDto(PolicyCategory policyCategory);
}
