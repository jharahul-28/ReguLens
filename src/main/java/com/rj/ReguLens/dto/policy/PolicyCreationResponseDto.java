package com.rj.ReguLens.dto.policy;

import com.rj.ReguLens.entity.PolicyCategory;

import java.util.Set;
import java.util.UUID;

public record PolicyCreationResponseDto(
        UUID id,
        String title,
        String description,
        Set<String> categories
) {
}
