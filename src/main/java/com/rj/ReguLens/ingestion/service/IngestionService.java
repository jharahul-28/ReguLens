package com.rj.ReguLens.ingestion.service;

import com.rj.ReguLens.entity.DocumentChunk;
import com.rj.ReguLens.ingestion.model.ExtractedClauseChunk;
import com.rj.ReguLens.ingestion.model.RawDocument;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface IngestionService {

    List<ExtractedClauseChunk> previewDocumentChunking(RawDocument rawDocument);

    List<ExtractedClauseChunk> parseAndChunkStream(InputStream inputStream, String contentType, String filename, String regulationName, String jurisdiction);

    List<DocumentChunk> processAndPersistPolicyVersion(UUID policyVersionId);
}
