package com.rj.ReguLens.embedding.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockEmbeddingAdapterTest {

    private MockEmbeddingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockEmbeddingAdapter();
    }

    @Test
    void shouldGenerateNormalizedVectorWithExpectedDimension() {
        String text = "Article 17: Right to erasure ('right to be forgotten')";
        float[] vector = adapter.embed(text);

        assertNotNull(vector);
        assertEquals(768, vector.length);
        assertEquals(768, adapter.getDimension());

        // Check L2 norm is approximately 1.0
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        assertEquals(1.0, Math.sqrt(norm), 0.001);
    }

    @Test
    void shouldBeDeterministicForSameInputText() {
        String text = "GDPR Chapter III Section 1";
        float[] vec1 = adapter.embed(text);
        float[] vec2 = adapter.embed(text);

        assertArrayEquals(vec1, vec2);
    }

    @Test
    void shouldGenerateBatchEmbeddings() {
        List<String> texts = List.of("Clause 1", "Clause 2", "Clause 3");
        List<float[]> vectors = adapter.embedAll(texts);

        assertEquals(3, vectors.size());
        for (float[] v : vectors) {
            assertEquals(768, v.length);
        }
    }
}
