package com.rj.ReguLens.dto;

import com.rj.ReguLens.dto.policy.PolicyResponse;
import com.rj.ReguLens.dto.policyVersion.PolicyVersionResponseDto;

import java.util.Set;

public record PolicyWithPolicyVersion(
        PolicyResponse policy,
        Set<PolicyVersionResponseDto> policyVersions
) {
}
