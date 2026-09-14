package com.rj.ReguLens.service;

import com.rj.ReguLens.dto.policy.PolicyCreationRequestDto;
import com.rj.ReguLens.dto.policy.PolicyCreationResponseDto;
import com.rj.ReguLens.dto.policy.PolicyUpdationRequestDto;
import com.rj.ReguLens.exception.BadRequestException;
import org.springframework.http.ResponseEntity;

public interface PolicyService {
    ResponseEntity<PolicyCreationResponseDto> createPolicy(PolicyCreationRequestDto policyCreationRequest) throws BadRequestException;

    ResponseEntity<PolicyCreationResponseDto> updatePolicy(PolicyUpdationRequestDto policyUpdationRequest) throws BadRequestException;
}
