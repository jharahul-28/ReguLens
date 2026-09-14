package com.rj.ReguLens.generation;

import com.rj.ReguLens.generation.model.LlmRequest;
import com.rj.ReguLens.generation.model.LlmResponse;

/**
 * Driven port for LLM text and chat generation.
 * Isolates domain and orchestration from vendor LLM SDKs.
 */
public interface LlmClientPort {

    LlmResponse generate(LlmRequest request);

    String getModelIdentifier();
}
