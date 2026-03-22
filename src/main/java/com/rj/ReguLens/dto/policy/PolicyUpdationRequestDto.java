package com.rj.ReguLens.dto.policy;

import java.util.UUID;

public record PolicyUpdationRequestDto(
        UUID id,
        String title,
        String description
) {
}
