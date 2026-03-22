package com.rj.ReguLens.mapper;

import com.rj.ReguLens.dto.policy.PolicyCreationResponseDto;
import com.rj.ReguLens.dto.policy.PolicyResponse;
import com.rj.ReguLens.entity.Policy;
import com.rj.ReguLens.entity.PolicyCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PolicyMapper {
    PolicyCreationResponseDto policyToPolicyCreationResponseDto(Policy policy);
    PolicyResponse policyToPolicyResponse(Policy policy);
    default String map(PolicyCategory category) {
        return category.getName();
    }
}
