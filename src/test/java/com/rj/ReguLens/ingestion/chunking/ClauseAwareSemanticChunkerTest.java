package com.rj.ReguLens.ingestion.chunking;

import com.rj.ReguLens.ingestion.model.ExtractedClauseChunk;
import com.rj.ReguLens.ingestion.model.RawDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClauseAwareSemanticChunkerTest {

    private ClauseAwareSemanticChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new ClauseAwareSemanticChunker();
    }

    @Test
    void shouldChunkRegulatoryDocumentWithChaptersAndArticles() {
        String gdprSample = """
                # Chapter III - Rights of the Data Subject
                
                ## Article 17 - Right to erasure ('right to be forgotten')
                
                (1) The data subject shall have the right to obtain from the controller the erasure of personal data concerning him or her without undue delay and the controller shall have the obligation to erase personal data without undue delay where one of the following grounds applies:
                
                (a) the personal data are no longer necessary in relation to the purposes for which they were collected or otherwise processed;
                
                (b) the data subject withdraws consent on which the processing is based according to point (a) of Article 6(1), or point (a) of Article 9(2), and where there is no other legal ground for the processing;
                
                ## Article 18 - Right to restriction of processing
                
                (1) The data subject shall have the right to obtain from the controller restriction of processing where one of the following applies:
                (a) the accuracy of the personal data is contested by the data subject.
                """;

        RawDocument rawDoc = RawDocument.builder()
                .title("GDPR Sample")
                .regulationName("GDPR")
                .jurisdiction("EU")
                .versionTag("v2024.1")
                .effectiveDate(LocalDate.of(2018, 5, 25))
                .content(gdprSample)
                .build();

        List<ExtractedClauseChunk> chunks = chunker.chunkDocument(rawDoc);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());

        // Verify that chunks have breadcrumb prefix
        for (ExtractedClauseChunk chunk : chunks) {
            assertTrue(chunk.getEnrichedContent().contains("[DOCUMENT: GDPR]"));
            assertTrue(chunk.getEnrichedContent().contains("[JURISDICTION: EU]"));
            assertTrue(chunk.getEnrichedContent().contains("[HIERARCHY:"));
            assertTrue(chunk.getEnrichedContent().contains("[CLAUSE:"));
            assertTrue(chunk.getTokenCount() > 0);
            assertNotNull(chunk.getMetadata().getChecksumSha256());
            assertEquals("EU", chunk.getMetadata().getJurisdiction());
            assertEquals("GDPR", chunk.getMetadata().getRegulationName());
        }

        // Verify hierarchy captures Chapter III and Article 17
        boolean foundArticle17 = chunks.stream()
                .anyMatch(c -> c.getSectionHierarchy().contains("Article 17") && c.getSectionHierarchy().contains("Chapter III"));
        assertTrue(foundArticle17, "Expected chunk with Article 17 hierarchy");
    }

    @Test
    void shouldHandleNumberedSectionsFormat() {
        String pciDssSample = """
                # Section 3 - Protect Cardholder Data
                
                3.1 Keep cardholder data storage to a minimum by implementing data retention and disposal policies.
                
                3.2 Do not store sensitive authentication data after authorization (even if encrypted). If sensitive authentication data is received, render all data unrecoverable upon completion of the authorization process.
                
                3.3 Mask PAN when displayed (the first six and last four digits are the maximum number of digits to be displayed), such that only personnel with a legitimate business need can see more than the first six/last four digits of the PAN.
                """;

        RawDocument rawDoc = RawDocument.builder()
                .title("PCI-DSS Requirements")
                .regulationName("PCI-DSS")
                .jurisdiction("GLOBAL")
                .versionTag("v4.0")
                .effectiveDate(LocalDate.of(2024, 3, 31))
                .content(pciDssSample)
                .build();

        List<ExtractedClauseChunk> chunks = chunker.chunkDocument(rawDoc);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(c -> c.getSectionHierarchy().contains("PCI-DSS")));
    }

    @Test
    void shouldHandleEmptyAndBlankDocumentsGracefully() {
        RawDocument emptyDoc = RawDocument.builder()
                .title("Empty")
                .content("")
                .build();

        List<ExtractedClauseChunk> chunks = chunker.chunkDocument(emptyDoc);
        assertTrue(chunks.isEmpty());

        RawDocument nullDoc = RawDocument.builder()
                .title("Null")
                .content(null)
                .build();

        List<ExtractedClauseChunk> nullChunks = chunker.chunkDocument(nullDoc);
        assertTrue(nullChunks.isEmpty());
    }

    @Test
    void shouldSplitLargeTextIntoOverlappingWindows() {
        StringBuilder largeClause = new StringBuilder("# Article 1 - Extremely Long Policy Clause\n\n");
        for (int i = 1; i <= 50; i++) {
            largeClause.append("Sentence number ").append(i)
                    .append(" establishes a critical compliance requirement that must be strictly audited under regulatory supervisory authority guidelines. ");
        }

        RawDocument doc = RawDocument.builder()
                .title("Large Doc")
                .regulationName("FINRA")
                .jurisdiction("US")
                .content(largeClause.toString())
                .build();

        // Target low token budget to trigger windowing
        List<ExtractedClauseChunk> chunks = chunker.chunkDocument(doc, 100, 20);

        assertTrue(chunks.size() > 1, "Expected multiple chunks due to token overflow");
        assertTrue(chunks.get(0).getClauseReference().contains("[Part 1/"));
    }
}
