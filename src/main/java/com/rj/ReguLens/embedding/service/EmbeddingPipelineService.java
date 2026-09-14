package com.rj.ReguLens.embedding.service;

import com.rj.ReguLens.entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

public interface EmbeddingPipelineService {

    List<DocumentChunk> generateAndPersistEmbeddingsForPolicyVersion(UUID policyVersionId);

    float[] generateEmbeddingForQuery(String queryText);
}
