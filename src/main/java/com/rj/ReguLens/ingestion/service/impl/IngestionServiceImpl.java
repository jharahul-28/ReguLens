package com.rj.ReguLens.ingestion.service.impl;

import com.rj.ReguLens.embedding.EmbeddingPort;
import com.rj.ReguLens.entity.DocumentChunk;
import com.rj.ReguLens.entity.Policy;
import com.rj.ReguLens.entity.PolicyCategory;
import com.rj.ReguLens.entity.PolicyVersion;
import com.rj.ReguLens.exception.ResourceNotFound;
import com.rj.ReguLens.ingestion.chunking.ClauseAwareSemanticChunker;
import com.rj.ReguLens.ingestion.model.ExtractedClauseChunk;
import com.rj.ReguLens.ingestion.model.RawDocument;
import com.rj.ReguLens.ingestion.parser.DocumentParserRegistry;
import com.rj.ReguLens.ingestion.service.IngestionService;
import com.rj.ReguLens.repository.DocumentChunkRepository;
import com.rj.ReguLens.repository.PolicyVersionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IngestionServiceImpl implements IngestionService {

    ClauseAwareSemanticChunker chunker;
    DocumentParserRegistry parserRegistry;
    PolicyVersionRepository policyVersionRepository;
    DocumentChunkRepository documentChunkRepository;
    EmbeddingPort embeddingPort;

    @Override
    public List<ExtractedClauseChunk> previewDocumentChunking(RawDocument rawDocument) {
        log.info("Previewing chunking for document: {}", rawDocument.getRegulationName());
        return chunker.chunkDocument(rawDocument);
    }

    @Override
    public List<ExtractedClauseChunk> parseAndChunkStream(
            InputStream inputStream,
            String contentType,
            String filename,
            String regulationName,
            String jurisdiction
    ) {
        try {
            String parsedText = parserRegistry.parse(inputStream, contentType, filename);
            RawDocument doc = RawDocument.builder()
                    .title(filename)
                    .regulationName(regulationName != null ? regulationName : filename)
                    .jurisdiction(jurisdiction != null ? jurisdiction : "GLOBAL")
                    .content(parsedText)
                    .contentType(contentType)
                    .effectiveDate(LocalDate.now())
                    .build();
            return chunker.chunkDocument(doc);
        } catch (IOException e) {
            log.error("Failed to parse input stream for file: {}", filename, e);
            throw new RuntimeException("Document parsing error: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public List<DocumentChunk> processAndPersistPolicyVersion(UUID policyVersionId) {
        log.info("Processing and indexing chunks for policy version ID: {}", policyVersionId);

        PolicyVersion policyVersion = policyVersionRepository.findById(policyVersionId)
                .orElseThrow(() -> new ResourceNotFound("Policy Version not found with id: " + policyVersionId));

        Policy policy = policyVersion.getPolicy();
        String policyTitle = policy != null ? policy.getTitle() : "Regulatory Policy";
        String categories = policy != null && policy.getCategories() != null
                ? policy.getCategories().stream().map(PolicyCategory::getName).collect(Collectors.joining(", "))
                : "General";

        LocalDate effectiveDate = policyVersion.getEffectiveFrom() != null
                ? policyVersion.getEffectiveFrom().toLocalDate()
                : LocalDate.now();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("policy_id", policy != null ? policy.getId().toString() : null);
        attributes.put("policy_version_id", policyVersion.getId().toString());
        attributes.put("version_number", policyVersion.getVersion());
        attributes.put("status", policyVersion.getStatus().name());
        attributes.put("categories", categories);

        RawDocument rawDocument = RawDocument.builder()
                .title(policyTitle)
                .regulationName(policyTitle)
                .jurisdiction("GLOBAL")
                .versionTag("v" + policyVersion.getVersion())
                .effectiveDate(effectiveDate)
                .content(policyVersion.getContent())
                .additionalAttributes(attributes)
                .build();

        List<ExtractedClauseChunk> extractedChunks = chunker.chunkDocument(rawDocument);
        log.info("Extracted {} clauses from policy version: {}", extractedChunks.size(), policyVersionId);

        // Delete existing chunks for idempotency
        documentChunkRepository.deleteByPolicyVersionId(policyVersionId);

        List<DocumentChunk> entitiesToSave = new ArrayList<>();
        for (ExtractedClauseChunk chunk : extractedChunks) {
            float[] vector = embeddingPort.embed(chunk.getEnrichedContent());
            Float[] boxedVector = new Float[vector.length];
            for (int i = 0; i < vector.length; i++) {
                boxedVector[i] = vector[i];
            }

            DocumentChunk entity = DocumentChunk.builder()
                    .policyVersion(policyVersion)
                    .chunkIndex(chunk.getChunkIndex())
                    .clauseReference(chunk.getClauseReference())
                    .sectionHierarchy(chunk.getSectionHierarchy())
                    .content(chunk.getEnrichedContent())
                    .tokenCount(chunk.getTokenCount())
                    .metadata(chunk.getMetadataMap())
                    .embedding(boxedVector)
                    .build();
            entitiesToSave.add(entity);
        }

        return documentChunkRepository.saveAll(entitiesToSave);
    }
}
