package com.rj.ReguLens.generation.prompt;

import com.rj.ReguLens.generation.model.LlmRequest;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegulatoryPromptBuilderTest {

    private RegulatoryPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new RegulatoryPromptBuilder();
    }

    @Test
    void shouldBuildPromptWithActivePolicyContext() {
        UUID chunkId = UUID.randomUUID();
        ScoredDocumentChunk chunk = ScoredDocumentChunk.builder()
                .chunkId(chunkId)
                .policyTitle("GDPR Standard")
                .versionNumber(2)
                .policyStatus("ACTIVE")
                .clauseReference("Article 17(1)")
                .sectionHierarchy("GDPR > Chapter III > Article 17")
                .content("The data subject shall have the right to obtain erasure of personal data.")
                .build();

        LlmRequest request = promptBuilder.buildRequest("How does erasure work?", List.of(chunk), false);

        assertNotNull(request);
        assertTrue(request.getSystemPrompt().contains("You are ReguLens"));
        assertTrue(request.getUserPrompt().contains("How does erasure work?"));
        assertTrue(request.getUserPrompt().contains("[CHUNK_ID: " + chunkId + "]"));
        assertTrue(request.getUserPrompt().contains("[STATUS: ACTIVE]"));
        assertFalse(request.getUserPrompt().contains("⚠️ Regulatory Notice: Policy"));
    }

    @Test
    void shouldInjectExpiredPolicyNoticeWhenOnlyExpiredMatches() {
        UUID chunkId = UUID.randomUUID();
        ScoredDocumentChunk chunk = ScoredDocumentChunk.builder()
                .chunkId(chunkId)
                .policyTitle("Legacy GDPR Guidelines")
                .versionNumber(1)
                .policyStatus("EXPIRED")
                .effectiveTo(LocalDateTime.of(2023, 12, 31, 23, 59))
                .clauseReference("Article 17")
                .content("Old erasure policy.")
                .build();

        LlmRequest request = promptBuilder.buildRequest("Erasure in 2023?", List.of(chunk), true);

        assertTrue(request.getUserPrompt().contains("⚠️ Regulatory Notice: Policy 'Legacy GDPR Guidelines' (Version 1) expired on 2023-12-31."));
    }
}
