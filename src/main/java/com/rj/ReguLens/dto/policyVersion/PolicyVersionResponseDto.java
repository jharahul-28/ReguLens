package com.rj.ReguLens.dto.policyVersion;

import com.rj.ReguLens.entity.PolicyVersionStatusEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record PolicyVersionResponseDto(
        UUID id,
        Integer version,
        UUID policyId,
        String content,
        UUID approvedById,
        PolicyVersionStatusEnum status,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo
) {
}
