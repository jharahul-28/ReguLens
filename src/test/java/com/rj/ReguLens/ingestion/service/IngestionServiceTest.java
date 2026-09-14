package com.rj.ReguLens.ingestion.service;

import com.rj.ReguLens.embedding.EmbeddingPort;
import com.rj.ReguLens.entity.*;
import com.rj.ReguLens.ingestion.chunking.ClauseAwareSemanticChunker;
import com.rj.ReguLens.ingestion.parser.DocumentParserRegistry;
import com.rj.ReguLens.ingestion.parser.PlainTextDocumentParser;
import com.rj.ReguLens.ingestion.service.impl.IngestionServiceImpl;
import com.rj.ReguLens.repository.DocumentChunkRepository;
import com.rj.ReguLens.repository.PolicyVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private PolicyVersionRepository policyVersionRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private EmbeddingPort embeddingPort;

    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ClauseAwareSemanticChunker chunker = new ClauseAwareSemanticChunker();
        PlainTextDocumentParser plainParser = new PlainTextDocumentParser();
        DocumentParserRegistry registry = new DocumentParserRegistry(List.of(plainParser), plainParser);

        when(embeddingPort.embed(anyString())).thenReturn(new float[768]);

        ingestionService = new IngestionServiceImpl(
                chunker,
                registry,
                policyVersionRepository,
                documentChunkRepository,
                embeddingPort
        );
    }

    @Test
    void shouldProcessAndPersistPolicyVersionChunks() {
        UUID versionId = UUID.randomUUID();
        Policy policy = Policy.builder()
                .id(UUID.randomUUID())
                .title("GDPR Compliance Standard")
                .categories(Set.of(PolicyCategory.builder().name("Privacy").build()))
                .build();

        PolicyVersion policyVersion = PolicyVersion.builder()
                .id(versionId)
                .policy(policy)
                .version(1)
                .status(PolicyVersionStatusEnum.ACTIVE)
                .content("""
                        # Chapter I - General Provisions
                        ## Article 1 - Subject-matter and objectives
                        (1) This Regulation lays down rules relating to the protection of natural persons with regard to the processing of personal data.
                        """)
                .effectiveFrom(LocalDateTime.now())
                .effectiveTo(LocalDateTime.now().plusYears(1))
                .build();

        when(policyVersionRepository.findById(versionId)).thenReturn(Optional.of(policyVersion));
        when(documentChunkRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<DocumentChunk> savedChunks = ingestionService.processAndPersistPolicyVersion(versionId);

        assertNotNull(savedChunks);
        assertFalse(savedChunks.isEmpty());
        verify(documentChunkRepository).deleteByPolicyVersionId(versionId);
        verify(documentChunkRepository).saveAll(anyList());

        DocumentChunk firstChunk = savedChunks.get(0);
        assertTrue(firstChunk.getContent().contains("[DOCUMENT: GDPR Compliance Standard]"));
        assertEquals(versionId, firstChunk.getPolicyVersion().getId());
    }
}
