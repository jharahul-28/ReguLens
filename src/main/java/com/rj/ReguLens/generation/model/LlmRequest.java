package com.rj.ReguLens.generation.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LlmRequest {
    String systemPrompt;
    String userPrompt;
    @Builder.Default
    double temperature = 0.0;
    @Builder.Default
    int maxTokens = 1024;
}
