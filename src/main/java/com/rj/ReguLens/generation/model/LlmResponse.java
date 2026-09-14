package com.rj.ReguLens.generation.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LlmResponse {
    String content;
    int promptTokens;
    int completionTokens;
    long latencyMs;
    String modelName;
}
