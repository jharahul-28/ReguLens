package com.rj.ReguLens.audit.controller;

import com.rj.ReguLens.audit.dto.AuditLogDetailDto;
import com.rj.ReguLens.audit.dto.AuditLogSummaryDto;
import com.rj.ReguLens.audit.model.AuditLogFilter;
import com.rj.ReguLens.audit.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditController {

    AuditService auditService;

    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogSummaryDto>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String groundednessStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuditLogFilter filter = AuditLogFilter.builder()
                .userId(userId)
                .groundednessStatus(groundednessStatus)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(auditService.getAuditLogs(filter, pageable));
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<AuditLogDetailDto> getAuditLogById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.getAuditLogById(id));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAuditReport(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime
    ) {
        if ("json".equalsIgnoreCase(format)) {
            byte[] jsonBytes = auditService.exportAuditReportJson(startTime, endTime);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"regulens-audit-report.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBytes);
        } else {
            byte[] csvBytes = auditService.exportAuditReportCsv(startTime, endTime);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"regulens-audit-report.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvBytes);
        }
    }
}
