package com.rj.ReguLens.embedding.adapter;

import com.rj.ReguLens.embedding.EmbeddingPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Feature-hashed subword & keyword embedding generator with stop-word pruning
 * for high-fidelity offline semantic retrieval matching.
 */
@Component("mockEmbeddingAdapter")
public class MockEmbeddingAdapter implements EmbeddingPort {

    private static final int DIMENSION = 768;
    private static final String MODEL_ID = "mock-semantic-hash-embedding-768";

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "can", "cannot", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself",
            "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "me", "more", "most", "my", "myself",
            "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves",
            "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their",
            "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while", "who",
            "whom", "why", "with", "would", "you", "your", "yours", "yourself", "yourselves"
    );

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[DIMENSION];
        }

        float[] vector = new float[DIMENSION];
        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] words = cleaned.split("\\s+");

        int contentWordCount = 0;
        for (String word : words) {
            if (word.length() < 2 || STOP_WORDS.contains(word)) continue;
            contentWordCount++;

            int h = word.hashCode();
            // Distribute energy across multiple dimensions to emulate dense semantic representations
            for (int k = 0; k < 12; k++) {
                int idx = Math.abs(h * (k * 31 + 17) + k * 97) % DIMENSION;
                float weight = (k % 3 == 0) ? 2.0f : ((k % 2 == 0) ? 1.5f : 1.0f);
                vector[idx] += weight;
            }

            // Character 3-grams for subword semantic matching
            if (word.length() >= 4) {
                for (int i = 0; i <= word.length() - 3; i++) {
                    String gram = word.substring(i, i + 3);
                    int gramHash = Math.abs(gram.hashCode() * 43) % DIMENSION;
                    vector[gramHash] += 0.5f;
                }
            }
        }

        // Fallback for short texts containing only stop words
        if (contentWordCount == 0) {
            for (String word : words) {
                if (word.isBlank()) continue;
                int idx = Math.abs(word.hashCode()) % DIMENSION;
                vector[idx] += 1.0f;
            }
        }

        // L2 Normalize
        float norm = 0.0f;
        for (int i = 0; i < DIMENSION; i++) {
            norm += vector[i] * vector[i];
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DIMENSION; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public String getModelIdentifier() {
        return MODEL_ID;
    }
}
