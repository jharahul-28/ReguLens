package com.rj.ReguLens.ingestion.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Value
@Builder
public class ChunkMetadata {
    String jurisdiction;
    String regulationName;
    String versionTag;
    LocalDate effectiveDate;
    String sectionHierarchy;
    String clauseReference;
    int chunkIndex;
    int tokenCount;
    String sourceUrl;
    String checksumSha256;
    Map<String, Object> customAttributes;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (jurisdiction != null) map.put("jurisdiction", jurisdiction);
        if (regulationName != null) map.put("regulation_name", regulationName);
        if (versionTag != null) map.put("version_tag", versionTag);
        if (effectiveDate != null) map.put("effective_date", effectiveDate.toString());
        if (sectionHierarchy != null) map.put("section_hierarchy", sectionHierarchy);
        if (clauseReference != null) map.put("clause_reference", clauseReference);
        map.put("chunk_index", chunkIndex);
        map.put("token_count", tokenCount);
        if (sourceUrl != null) map.put("source_url", sourceUrl);
        if (checksumSha256 != null) map.put("checksum_sha256", checksumSha256);
        if (customAttributes != null) {
            map.putAll(customAttributes);
        }
        return map;
    }
}
