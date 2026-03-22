package com.rj.ReguLens.dto.policy;

import java.util.Set;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String title,
        String description,
        Set<String> categories,
        Boolean active
) {
}
