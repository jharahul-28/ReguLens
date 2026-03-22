package com.rj.ReguLens.service;

import com.rj.ReguLens.dto.policy.PolicyCategoryCreationRequestDto;
import com.rj.ReguLens.dto.policy.PolicyCategoryCreationResponseDto;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;

public interface PolicyCategoryService {
    ResponseEntity<PolicyCategoryCreationResponseDto> createPolicyCategory(PolicyCategoryCreationRequestDto policyCategoryCreationRequestDto) throws BadRequestException;
}
