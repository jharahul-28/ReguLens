package com.rj.ReguLens.service;

import com.rj.ReguLens.dto.policyVersion.PolicyVersionCreationRequestDto;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionResponseDto;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionUpdationRequestDto;
import com.rj.ReguLens.dto.PolicyWithPolicyVersion;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface PolicyVersionService {
    ResponseEntity<PolicyWithPolicyVersion> addPolicyVersion(UUID policyId, PolicyVersionCreationRequestDto policyVersionCreationRequestDto) throws BadRequestException;

    ResponseEntity<PolicyVersionResponseDto> updatePolicyVersion(UUID id, PolicyVersionUpdationRequestDto policyVersionUpdationRequestDto) throws BadRequestException;

    ResponseEntity<String> approvePolicyVersion(UUID id);

    ResponseEntity<String> activatePolicyVersion(UUID id);

    PolicyVersionResponseDto getPolicyVersionById(UUID policyId);
}
