package com.rj.ReguLens.embedding;

import java.util.List;

/**
 * Driven port for swappable vector embedding models.
 * Isolates domain/application logic from concrete AI SDKs (LangChain4j/Spring AI).
 */
public interface EmbeddingPort {

    float[] embed(String text);

    List<float[]> embedAll(List<String> texts);

    int getDimension();

    String getModelIdentifier();
}
