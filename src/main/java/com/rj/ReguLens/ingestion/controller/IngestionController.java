package com.rj.ReguLens.ingestion.controller;

import com.rj.ReguLens.entity.DocumentChunk;
import com.rj.ReguLens.ingestion.dto.ChunkPreviewRequestDto;
import com.rj.ReguLens.ingestion.dto.ChunkResponseDto;
import com.rj.ReguLens.ingestion.dto.IngestionSummaryResponseDto;
import com.rj.ReguLens.ingestion.model.ExtractedClauseChunk;
import com.rj.ReguLens.ingestion.model.RawDocument;
import com.rj.ReguLens.ingestion.service.IngestionService;
import com.rj.ReguLens.ingestion.seed.SamplePolicySeeder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ingestion")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IngestionController {

    IngestionService ingestionService;
    SamplePolicySeeder policySeeder;

    @PostMapping("/seed-sample-policies")
    public ResponseEntity<Map<String, Object>> seedSamplePolicies() {
        log.info("Received request to seed sample regulatory policies (GDPR, HIPAA, Expired Legacy).");
        int count = policySeeder.seedSamplePolicies();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Successfully ingested and indexed GDPR, HIPAA, and Expired Legacy sample policies into Neon pgvector.",
                "totalChunksCreated", count
        ));
    }

    @PostMapping("/preview")
    public ResponseEntity<List<ChunkResponseDto>> previewChunking(@RequestBody ChunkPreviewRequestDto requestDto) {
        log.info("Received request to preview chunking for: {}", requestDto.regulationName());
        RawDocument rawDocument = RawDocument.builder()
                .title(requestDto.title())
                .regulationName(requestDto.regulationName())
                .jurisdiction(requestDto.jurisdiction())
                .versionTag(requestDto.versionTag())
                .effectiveDate(requestDto.effectiveDate())
                .content(requestDto.content())
                .build();

        List<ExtractedClauseChunk> extracted = ingestionService.previewDocumentChunking(rawDocument);

        List<ChunkResponseDto> response = extracted.stream()
                .map(c -> new ChunkResponseDto(
                        null,
                        c.getChunkIndex(),
                        c.getClauseReference(),
                        c.getSectionHierarchy(),
                        c.getEnrichedContent(),
                        c.getTokenCount(),
                        c.getMetadataMap()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ChunkResponseDto>> uploadAndChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "regulationName", required = false) String regulationName,
            @RequestParam(value = "jurisdiction", defaultValue = "GLOBAL") String jurisdiction
    ) throws IOException {
        log.info("Received file upload for ingestion: {}", file.getOriginalFilename());
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.txt";
        String contentType = file.getContentType() != null ? file.getContentType() : "text/plain";

        List<ExtractedClauseChunk> extracted = ingestionService.parseAndChunkStream(
                file.getInputStream(),
                contentType,
                filename,
                regulationName != null ? regulationName : filename,
                jurisdiction
        );

        List<ChunkResponseDto> response = extracted.stream()
                .map(c -> new ChunkResponseDto(
                        null,
                        c.getChunkIndex(),
                        c.getClauseReference(),
                        c.getSectionHierarchy(),
                        c.getEnrichedContent(),
                        c.getTokenCount(),
                        c.getMetadataMap()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/policy-version/{policyVersionId}/process")
    public ResponseEntity<IngestionSummaryResponseDto> processPolicyVersion(@PathVariable UUID policyVersionId) {
        log.info("Triggering chunk processing for policy version: {}", policyVersionId);
        List<DocumentChunk> savedChunks = ingestionService.processAndPersistPolicyVersion(policyVersionId);

        int totalTokens = savedChunks.stream().mapToInt(DocumentChunk::getTokenCount).sum();

        List<ChunkResponseDto> chunkDtos = savedChunks.stream()
                .map(c -> new ChunkResponseDto(
                        c.getId(),
                        c.getChunkIndex(),
                        c.getClauseReference(),
                        c.getSectionHierarchy(),
                        c.getContent(),
                        c.getTokenCount(),
                        c.getMetadata()
                ))
                .toList();

        IngestionSummaryResponseDto summary = new IngestionSummaryResponseDto(
                policyVersionId,
                savedChunks.size(),
                totalTokens,
                chunkDtos
        );

        return ResponseEntity.ok(summary);
    }
}
