package com.rj.ReguLens.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rj.ReguLens.audit.dto.AuditLogDetailDto;
import com.rj.ReguLens.audit.dto.AuditLogSummaryDto;
import com.rj.ReguLens.audit.model.AuditLogFilter;
import com.rj.ReguLens.audit.model.AuditRecordCommand;
import com.rj.ReguLens.audit.service.impl.AuditServiceImpl;
import com.rj.ReguLens.entity.QueryAuditLog;
import com.rj.ReguLens.exception.ResourceNotFound;
import com.rj.ReguLens.generation.model.AttributedClaim;
import com.rj.ReguLens.generation.model.GroundednessStatus;
import com.rj.ReguLens.repository.QueryAuditLogRepository;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private QueryAuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        auditService = new AuditServiceImpl(auditLogRepository, mapper);
    }

    @Test
    void shouldRecordAuditEntryWithSerializedChunksAndClaims() {
        UUID chunkId = UUID.randomUUID();
        ScoredDocumentChunk chunk = ScoredDocumentChunk.builder()
                .chunkId(chunkId)
                .policyTitle("GDPR Standard")
                .versionNumber(2)
                .policyStatus("ACTIVE")
                .clauseReference("Article 17")
                .finalRelevanceScore(0.88)
                .build();

        AttributedClaim claim = AttributedClaim.builder()
                .statement("Erasure right exists.")
                .citedChunkId(chunkId)
                .clauseReference("Article 17")
                .verified(true)
                .verificationReason("Entailed")
                .build();

        AuditRecordCommand command = AuditRecordCommand.builder()
                .queryText("Erasure inquiry")
                .retrievedChunks(List.of(chunk))
                .similarityThreshold(0.70)
                .rawPrompt("Prompt text")
                .generatedAnswer("Erasure answer [Ref: " + chunkId + "].")
                .claims(List.of(claim))
                .groundednessStatus(GroundednessStatus.VERIFIED_ENTAILED)
                .modelIdentifier("gemini-1.5-pro")
                .promptTokens(100)
                .completionTokens(50)
                .retrievalLatencyMs(80)
                .generationLatencyMs(450)
                .totalLatencyMs(530)
                .build();

        when(auditLogRepository.save(any(QueryAuditLog.class))).thenAnswer(inv -> {
            QueryAuditLog saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(OffsetDateTime.now());
            return saved;
        });

        QueryAuditLog result = auditService.recordAuditEntry(command);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Erasure inquiry", result.getQueryText());
        assertEquals("VERIFIED_ENTAILED", result.getGroundednessStatus());
        assertEquals(1, result.getRetrievedChunks().size());
        assertEquals(1, result.getStructuredCitations().size());
        verify(auditLogRepository).save(any(QueryAuditLog.class));
    }

    @Test
    void shouldGetAuditLogsWithPagination() {
        QueryAuditLog logEntity = QueryAuditLog.builder()
                .id(UUID.randomUUID())
                .queryText("Retention period?")
                .generatedAnswer("Retention period is 5 years.")
                .groundednessStatus("VERIFIED_ENTAILED")
                .retrievedChunks(List.of(Map.of("chunk_id", UUID.randomUUID().toString())))
                .structuredCitations(List.of(Map.of("statement", "Retention is 5 years")))
                .totalLatencyMs(350L)
                .modelIdentifier("gemini-1.5-pro")
                .createdAt(OffsetDateTime.now())
                .build();

        Page<QueryAuditLog> page = new PageImpl<>(List.of(logEntity));
        when(auditLogRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        Page<AuditLogSummaryDto> summaries = auditService.getAuditLogs(AuditLogFilter.builder().build(), PageRequest.of(0, 10));

        assertNotNull(summaries);
        assertEquals(1, summaries.getTotalElements());
        assertEquals("Retention period?", summaries.getContent().get(0).queryText());
    }

    @Test
    void shouldGetAuditLogById() {
        UUID id = UUID.randomUUID();
        QueryAuditLog logEntity = QueryAuditLog.builder()
                .id(id)
                .queryText("Right to be forgotten")
                .similarityThreshold(BigDecimal.valueOf(0.70))
                .retrievedChunks(List.of())
                .structuredCitations(List.of())
                .rawPrompt("Prompt")
                .generatedAnswer("Answer")
                .groundednessStatus("VERIFIED_ENTAILED")
                .modelIdentifier("gemini-1.5-pro")
                .modelVersion("1.0")
                .promptTokens(10)
                .completionTokens(20)
                .retrievalLatencyMs(10L)
                .generationLatencyMs(20L)
                .totalLatencyMs(30L)
                .createdAt(OffsetDateTime.now())
                .build();

        when(auditLogRepository.findById(id)).thenReturn(Optional.of(logEntity));

        AuditLogDetailDto detail = auditService.getAuditLogById(id);

        assertNotNull(detail);
        assertEquals(id, detail.id());
        assertEquals("Right to be forgotten", detail.queryText());
    }

    @Test
    void shouldThrowWhenAuditLogNotFound() {
        UUID id = UUID.randomUUID();
        when(auditLogRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFound.class, () -> auditService.getAuditLogById(id));
    }

    @Test
    void shouldExportAuditReportCsvAndJson() {
        QueryAuditLog logEntity = QueryAuditLog.builder()
                .id(UUID.randomUUID())
                .queryText("Query for export")
                .similarityThreshold(BigDecimal.valueOf(0.70))
                .rawPrompt("Prompt")
                .generatedAnswer("Answer for export")
                .retrievedChunks(List.of())
                .structuredCitations(List.of())
                .groundednessStatus("VERIFIED_ENTAILED")
                .modelIdentifier("gemini-1.5-pro")
                .modelVersion("1.0")
                .promptTokens(10)
                .completionTokens(20)
                .retrievalLatencyMs(10L)
                .generationLatencyMs(20L)
                .totalLatencyMs(30L)
                .createdAt(OffsetDateTime.now())
                .build();

        when(auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(logEntity));

        byte[] csv = auditService.exportAuditReportCsv(null, null);
        assertNotNull(csv);
        String csvString = new String(csv, StandardCharsets.UTF_8);
        assertTrue(csvString.contains("Log ID"));
        assertTrue(csvString.contains("Query for export"));

        byte[] json = auditService.exportAuditReportJson(null, null);
        assertNotNull(json);
        String jsonString = new String(json, StandardCharsets.UTF_8);
        assertTrue(jsonString.contains("Query for export"));
    }
}
