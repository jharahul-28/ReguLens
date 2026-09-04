package com.rj.ReguLens.embedding.service;

import com.rj.ReguLens.embedding.adapter.MockEmbeddingAdapter;
import com.rj.ReguLens.embedding.service.impl.EmbeddingPipelineServiceImpl;
import com.rj.ReguLens.entity.DocumentChunk;
import com.rj.ReguLens.entity.PolicyVersion;
import com.rj.ReguLens.ingestion.service.IngestionService;
import com.rj.ReguLens.repository.DocumentChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingPipelineServiceTest {

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private IngestionService ingestionService;

    private EmbeddingPipelineService pipelineService;

    @BeforeEach
    void setUp() {
        MockEmbeddingAdapter mockAdapter = new MockEmbeddingAdapter();
        pipelineService = new EmbeddingPipelineServiceImpl(
                mockAdapter,
                documentChunkRepository,
                ingestionService
        );
    }

    @Test
    void shouldGenerateAndPersistEmbeddingsForExistingChunks() {
        UUID versionId = UUID.randomUUID();
        PolicyVersion pv = PolicyVersion.builder().id(versionId).build();

        DocumentChunk chunk1 = DocumentChunk.builder()
                .id(UUID.randomUUID())
                .policyVersion(pv)
                .chunkIndex(1)
                .content("[DOCUMENT: GDPR] Article 17")
                .build();

        DocumentChunk chunk2 = DocumentChunk.builder()
                .id(UUID.randomUUID())
                .policyVersion(pv)
                .chunkIndex(2)
                .content("[DOCUMENT: GDPR] Article 18")
                .build();

        when(documentChunkRepository.findByPolicyVersionIdOrderByChunkIndexAsc(versionId))
                .thenReturn(List.of(chunk1, chunk2));
        when(documentChunkRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<DocumentChunk> result = pipelineService.generateAndPersistEmbeddingsForPolicyVersion(versionId);

        assertEquals(2, result.size());
        assertNotNull(result.get(0).getEmbedding());
        assertEquals(768, result.get(0).getEmbedding().length);
        verify(documentChunkRepository).saveAll(anyList());
    }

    @Test
    void shouldGenerateEmbeddingForSingleQuery() {
        float[] queryVec = pipelineService.generateEmbeddingForQuery("How to handle data erasure?");
        assertNotNull(queryVec);
        assertEquals(768, queryVec.length);
    }
}
