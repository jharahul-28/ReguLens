package com.rj.ReguLens.ingestion.parser;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentParser {

    boolean supports(String contentType, String filename);

    String parseToString(InputStream inputStream, String filename) throws IOException;
}
