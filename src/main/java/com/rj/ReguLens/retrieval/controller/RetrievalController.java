package com.rj.ReguLens.retrieval.controller;

import com.rj.ReguLens.entity.DocumentChunk;
import com.rj.ReguLens.embedding.service.EmbeddingPipelineService;
import com.rj.ReguLens.retrieval.dto.RetrievalRequestDto;
import com.rj.ReguLens.retrieval.dto.RetrievalResponseDto;
import com.rj.ReguLens.retrieval.dto.RetrievedChunkDto;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import com.rj.ReguLens.retrieval.model.RetrievalResult;
import com.rj.ReguLens.retrieval.service.RetrievalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/retrieval")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RetrievalController {

    RetrievalService retrievalService;
    EmbeddingPipelineService embeddingPipelineService;

    @PostMapping("/search")
    public ResponseEntity<RetrievalResponseDto> search(@RequestBody RetrievalRequestDto requestDto) {
        log.info("Received compliance search request for query: '{}'", requestDto.query());

        RetrievalFilter filter = RetrievalFilter.builder()
                .jurisdiction(requestDto.jurisdiction())
                .regulationName(requestDto.regulationName())
                .policyId(requestDto.policyId())
                .policyVersionId(requestDto.policyVersionId())
                .onlyActive(requestDto.onlyActive() != null ? requestDto.onlyActive() : false)
                .minSimilarityScore(requestDto.minSimilarityScore() != null ? requestDto.minSimilarityScore() : 0.70)
                .maxResults(requestDto.maxResults() != null ? requestDto.maxResults() : 5)
                .build();

        RetrievalResult result = retrievalService.retrieveRelevantClauses(requestDto.query(), filter);

        List<RetrievedChunkDto> chunkDtos = result.getChunks().stream()
                .map(c -> new RetrievedChunkDto(
                        c.getChunkId(),
                        c.getPolicyVersionId(),
                        c.getPolicyId(),
                        c.getPolicyTitle(),
                        c.getVersionNumber(),
                        c.getPolicyStatus(),
                        c.isExpired(),
                        c.getEffectiveFrom(),
                        c.getEffectiveTo(),
                        c.getClauseReference(),
                        c.getSectionHierarchy(),
                        c.getContent(),
                        c.getFinalRelevanceScore(),
                        c.getVectorScore(),
                        c.getFtsScore(),
                        c.getMetadata()
                ))
                .toList();

        RetrievalResponseDto response = new RetrievalResponseDto(
                result.getQueryText(),
                chunkDtos.size(),
                result.getMaxScore(),
                result.getThreshold(),
                result.isClearsThreshold(),
                result.isOnlyExpiredFound(),
                result.getExecutionTimeMs(),
                chunkDtos
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/policy-version/{policyVersionId}/embed")
    public ResponseEntity<String> embedPolicyVersion(@PathVariable UUID policyVersionId) {
        log.info("Triggering vector embedding generation for policy version: {}", policyVersionId);
        List<DocumentChunk> embedded = embeddingPipelineService.generateAndPersistEmbeddingsForPolicyVersion(policyVersionId);
        return ResponseEntity.ok("Successfully generated and persisted " + embedded.size() + " vector embeddings.");
    }
}
