package com.rj.ReguLens.generation.controller;

import com.rj.ReguLens.generation.dto.AttributedClaimDto;
import com.rj.ReguLens.generation.dto.ComplianceQueryRequestDto;
import com.rj.ReguLens.generation.dto.ComplianceQueryResponseDto;
import com.rj.ReguLens.generation.model.ComplianceAnswer;
import com.rj.ReguLens.generation.service.ComplianceGenerationService;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/generation")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenerationController {

    ComplianceGenerationService generationService;

    @PostMapping("/answer")
    public ResponseEntity<ComplianceQueryResponseDto> answerComplianceQuestion(@RequestBody ComplianceQueryRequestDto requestDto) {
        log.info("Received compliance answering request for question: '{}'", requestDto.question());

        RetrievalFilter filter = RetrievalFilter.builder()
                .jurisdiction(requestDto.jurisdiction())
                .regulationName(requestDto.regulationName())
                .policyId(requestDto.policyId())
                .onlyActive(requestDto.onlyActive() != null ? requestDto.onlyActive() : false)
                .minSimilarityScore(requestDto.minSimilarityScore() != null ? requestDto.minSimilarityScore() : 0.70)
                .maxResults(requestDto.maxContextChunks() != null ? requestDto.maxContextChunks() : 5)
                .build();

        ComplianceAnswer answer = generationService.answerComplianceQuery(requestDto.question(), filter);

        List<AttributedClaimDto> claimDtos = answer.getClaims().stream()
                .map(c -> new AttributedClaimDto(
                        c.getStatement(),
                        c.getCitedChunkId(),
                        c.getClauseReference(),
                        c.isVerified(),
                        c.getVerificationReason()
                ))
                .toList();

        ComplianceQueryResponseDto responseDto = new ComplianceQueryResponseDto(
                answer.getQueryText(),
                answer.getAnswer(),
                answer.isHasActivePolicy(),
                answer.isHasExpiredPolicyNotice(),
                answer.isClearsConfidenceThreshold(),
                answer.getRetrievalScore(),
                answer.getGroundednessStatus().name(),
                answer.getGroundednessConfidence(),
                claimDtos,
                answer.getCitedChunkIds(),
                answer.getTotalLatencyMs(),
                answer.getRetrievalLatencyMs(),
                answer.getGenerationLatencyMs(),
                answer.getPromptTokens(),
                answer.getCompletionTokens(),
                answer.getModelIdentifier()
        );

        return ResponseEntity.ok(responseDto);
    }
}
