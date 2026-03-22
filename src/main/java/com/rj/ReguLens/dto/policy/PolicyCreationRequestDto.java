package com.rj.ReguLens.dto.policy;

import java.util.Set;

public record PolicyCreationRequestDto(
        String title,
        String description,
        Set<String> categories
) {
}
