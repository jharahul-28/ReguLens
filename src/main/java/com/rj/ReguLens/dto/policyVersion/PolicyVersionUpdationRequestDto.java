package com.rj.ReguLens.dto.policyVersion;

import java.time.LocalDateTime;

public record PolicyVersionUpdationRequestDto(
        String content,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo
) {
}
