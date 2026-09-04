package com.rj.ReguLens.audit.service;

import com.rj.ReguLens.audit.dto.AuditLogDetailDto;
import com.rj.ReguLens.audit.dto.AuditLogSummaryDto;
import com.rj.ReguLens.audit.model.AuditLogFilter;
import com.rj.ReguLens.audit.model.AuditRecordCommand;
import com.rj.ReguLens.entity.QueryAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditService {

    QueryAuditLog recordAuditEntry(AuditRecordCommand command);

    Page<AuditLogSummaryDto> getAuditLogs(AuditLogFilter filter, Pageable pageable);

    AuditLogDetailDto getAuditLogById(UUID id);

    byte[] exportAuditReportCsv(OffsetDateTime start, OffsetDateTime end);

    byte[] exportAuditReportJson(OffsetDateTime start, OffsetDateTime end);
}
