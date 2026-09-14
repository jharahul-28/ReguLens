package com.rj.ReguLens.ingestion.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {

    private DocumentParserRegistry registry;

    @BeforeEach
    void setUp() {
        PlainTextDocumentParser plainParser = new PlainTextDocumentParser();
        MarkdownDocumentParser mdParser = new MarkdownDocumentParser();
        registry = new DocumentParserRegistry(List.of(plainParser, mdParser), plainParser);
    }

    @Test
    void shouldParsePlainTextStream() throws IOException {
        String input = "Section 1: General provisions.\nClause 1.1: Data processing rules.";
        ByteArrayInputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        String result = registry.parse(is, "text/plain", "policy.txt");
        assertEquals(input, result);
    }

    @Test
    void shouldParseMarkdownStream() throws IOException {
        String input = "# GDPR\n## Article 5\nPrinciples relating to processing of personal data.";
        ByteArrayInputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        String result = registry.parse(is, "text/markdown", "gdpr.md");
        assertEquals(input, result);
    }
}
