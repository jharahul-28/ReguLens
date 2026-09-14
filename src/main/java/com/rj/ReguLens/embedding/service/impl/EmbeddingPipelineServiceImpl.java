package com.rj.ReguLens.embedding.service.impl;

import com.rj.ReguLens.embedding.EmbeddingPort;
import com.rj.ReguLens.embedding.service.EmbeddingPipelineService;
import com.rj.ReguLens.entity.DocumentChunk;
import com.rj.ReguLens.ingestion.service.IngestionService;
import com.rj.ReguLens.repository.DocumentChunkRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class EmbeddingPipelineServiceImpl implements EmbeddingPipelineService {

    EmbeddingPort embeddingPort;
    DocumentChunkRepository documentChunkRepository;
    IngestionService ingestionService;

    private static final int BATCH_SIZE = 25;

    @Transactional
    @Override
    public List<DocumentChunk> generateAndPersistEmbeddingsForPolicyVersion(UUID policyVersionId) {
        log.info("Generating embeddings for policy version: {}", policyVersionId);

        List<DocumentChunk> chunks = documentChunkRepository.findByPolicyVersionIdOrderByChunkIndexAsc(policyVersionId);
        if (chunks.isEmpty()) {
            log.info("No chunks found for policy version {}, triggering ingestion first.", policyVersionId);
            chunks = ingestionService.processAndPersistPolicyVersion(policyVersionId);
        }

        if (chunks.isEmpty()) {
            log.warn("No chunks generated for policy version: {}", policyVersionId);
            return List.of();
        }

        List<String> textsToEmbed = chunks.stream().map(DocumentChunk::getContent).toList();
        List<float[]> embeddings = new ArrayList<>();

        // Process in batches
        for (int i = 0; i < textsToEmbed.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, textsToEmbed.size());
            List<String> batch = textsToEmbed.subList(i, end);
            log.debug("Embedding batch {} to {} for policy version {}", i, end, policyVersionId);
            embeddings.addAll(embeddingPort.embedAll(batch));
        }

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            float[] vec = embeddings.get(i);
            chunk.setEmbedding(boxFloatArray(vec));
        }

        List<DocumentChunk> saved = documentChunkRepository.saveAll(chunks);
        log.info("Successfully generated and saved {} vector embeddings for policy version: {}", saved.size(), policyVersionId);
        return saved;
    }

    @Override
    public float[] generateEmbeddingForQuery(String queryText) {
        return embeddingPort.embed(queryText);
    }

    public static Float[] boxFloatArray(float[] primitive) {
        if (primitive == null) return null;
        Float[] boxed = new Float[primitive.length];
        for (int i = 0; i < primitive.length; i++) {
            boxed[i] = primitive[i];
        }
        return boxed;
    }

    public static float[] unboxFloatArray(Float[] boxed) {
        if (boxed == null) return null;
        float[] primitive = new float[boxed.length];
        for (int i = 0; i < boxed.length; i++) {
            primitive[i] = boxed[i] != null ? boxed[i] : 0.0f;
        }
        return primitive;
    }
}
