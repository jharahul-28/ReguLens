package com.rj.ReguLens.ingestion.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ExtractedClauseChunk {
    int chunkIndex;
    String clauseReference;
    String sectionHierarchy;
    String rawContent;
    String enrichedContent;
    int tokenCount;
    ChunkMetadata metadata;

    public Map<String, Object> getMetadataMap() {
        return metadata != null ? metadata.toMap() : Map.of();
    }
}
