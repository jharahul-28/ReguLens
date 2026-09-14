package com.rj.ReguLens.ingestion.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Map;

@Value
@Builder
public class RawDocument {
    String title;
    String content;
    String jurisdiction;
    String regulationName;
    String versionTag;
    LocalDate effectiveDate;
    String sourceUrl;
    String contentType;
    Map<String, Object> additionalAttributes;
}
