package com.rj.ReguLens.dto.policyVersion;

import java.time.LocalDateTime;

public record PolicyVersionCreationRequestDto(
        String content,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo
) {
}
