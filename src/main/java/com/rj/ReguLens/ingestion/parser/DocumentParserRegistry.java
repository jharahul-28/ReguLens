package com.rj.ReguLens.ingestion.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentParserRegistry {

    private final List<DocumentParser> parsers;
    private final PlainTextDocumentParser defaultParser;

    public String parse(InputStream inputStream, String contentType, String filename) throws IOException {
        for (DocumentParser parser : parsers) {
            if (parser.supports(contentType, filename)) {
                return parser.parseToString(inputStream, filename);
            }
        }
        return defaultParser.parseToString(inputStream, filename);
    }
}
