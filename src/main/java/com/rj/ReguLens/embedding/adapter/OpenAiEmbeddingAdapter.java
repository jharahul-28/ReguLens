package com.rj.ReguLens.embedding.adapter;

import com.rj.ReguLens.embedding.EmbeddingPort;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("openAiEmbeddingAdapter")
@Primary
@ConditionalOnProperty(name = "regulens.ai.embedding.provider", havingValue = "openai")
@Slf4j
public class OpenAiEmbeddingAdapter implements EmbeddingPort {

    private static final String MODEL_NAME = "text-embedding-3-small";
    private static final int DIMENSION = 768;

    private final EmbeddingModel embeddingModel;
    private final MockEmbeddingAdapter fallbackAdapter;

    public OpenAiEmbeddingAdapter(
            @Value("${regulens.ai.embedding.api-key:${OPENAI_API_KEY:}}") String apiKey,
            MockEmbeddingAdapter fallbackAdapter
    ) {
        this.fallbackAdapter = fallbackAdapter;
        if (apiKey != null && !apiKey.isBlank() && !apiKey.equals("test-key")) {
            log.info("Initializing OpenAiEmbeddingModel with model: {}", MODEL_NAME);
            this.embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .modelName(MODEL_NAME)
                    .dimensions(DIMENSION)
                    .build();
        } else {
            log.warn("OpenAI API key is not configured. Operating in fallback mock embedding mode.");
            this.embeddingModel = null;
        }
    }

    @Override
    public float[] embed(String text) {
        if (embeddingModel == null) {
            return fallbackAdapter.embed(text);
        }
        try {
            Embedding embedding = embeddingModel.embed(TextSegment.from(text)).content();
            return embedding.vector();
        } catch (Exception e) {
            log.error("Error during OpenAI embedding call, falling back to deterministic vector: {}", e.getMessage());
            return fallbackAdapter.embed(text);
        }
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (embeddingModel == null) {
            return fallbackAdapter.embedAll(texts);
        }
        try {
            List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            return embeddings.stream().map(Embedding::vector).toList();
        } catch (Exception e) {
            log.error("Error during OpenAI batch embedding call, falling back to deterministic vectors: {}", e.getMessage());
            return fallbackAdapter.embedAll(texts);
        }
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public String getModelIdentifier() {
        return "openai-" + MODEL_NAME;
    }
}
