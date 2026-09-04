package com.rj.ReguLens.audit.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class AuditLogFilter {
    UUID userId;
    String groundednessStatus;
    OffsetDateTime startTime;
    OffsetDateTime endTime;
}
