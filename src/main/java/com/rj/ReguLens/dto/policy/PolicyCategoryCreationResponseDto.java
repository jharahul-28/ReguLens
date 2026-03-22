package com.rj.ReguLens.dto.policy;

import java.util.UUID;

public record PolicyCategoryCreationResponseDto(
        UUID id,
        String name
) {
}
