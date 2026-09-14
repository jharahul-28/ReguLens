package com.rj.ReguLens.ingestion.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class MarkdownDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String contentType, String filename) {
        if (contentType != null && (contentType.equalsIgnoreCase("text/markdown") || contentType.equalsIgnoreCase("text/x-markdown"))) {
            return true;
        }
        return filename != null && (filename.toLowerCase().endsWith(".md") || filename.toLowerCase().endsWith(".markdown"));
    }

    @Override
    public String parseToString(InputStream inputStream, String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
