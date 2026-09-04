package com.rj.ReguLens.generation.adapter;

import com.rj.ReguLens.generation.LlmClientPort;
import com.rj.ReguLens.generation.model.LlmRequest;
import com.rj.ReguLens.generation.model.LlmResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic Mock LLM client for testing and offline development without API keys.
 */
@Component("mockChatAdapter")
public class MockChatAdapter implements LlmClientPort {

    private static final String MODEL_NAME = "mock-deterministic-llm";
    private static final Pattern CHUNK_BLOCK_PATTERN = Pattern.compile(
            "\\[CHUNK_ID:\\s*([a-f0-9\\-]+)\\][\\s\\S]*?\\[CONTENT\\]:\\s*\\n([^\\n]+(?:\\n(?!---|--- CHUNK|\\[CHUNK_ID)[^\\n]+)*)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public LlmResponse generate(LlmRequest request) {
        long start = System.currentTimeMillis();
        String userPrompt = request.getUserPrompt() != null ? request.getUserPrompt() : "";

        Matcher matcher = CHUNK_BLOCK_PATTERN.matcher(userPrompt);
        List<String> generatedSentences = new ArrayList<>();

        while (matcher.find()) {
            String chunkId = matcher.group(1).trim();
            String content = matcher.group(2).trim();

            // Extract first meaningful sentence from chunk content
            String[] rawSentences = content.split("(?<=[.!?])\\s+");
            String statement = rawSentences.length > 0 && !rawSentences[0].isBlank()
                    ? rawSentences[0].trim()
                    : content;

            if (!statement.endsWith(".")) {
                statement += ".";
            }

            // Remove trailing period for citation placement
            String cleanStatement = statement.replaceAll("\\.+$", "");
            generatedSentences.add(cleanStatement + " [Ref: " + chunkId + "].");
        }

        String answer;
        if (generatedSentences.isEmpty()) {
            answer = "The indexed regulatory policy documents do not contain sufficient context to answer this compliance question.";
        } else {
            StringBuilder sb = new StringBuilder();
            if (userPrompt.contains("⚠️ Regulatory Notice: Policy")) {
                int noticeStart = userPrompt.indexOf("⚠️ Regulatory Notice: Policy");
                int noticeEnd = userPrompt.indexOf("\n\n", noticeStart);
                if (noticeEnd != -1) {
                    sb.append(userPrompt, noticeStart, noticeEnd).append("\n\n");
                }
            }
            sb.append(String.join(" ", generatedSentences));
            answer = sb.toString();
        }

        long latency = System.currentTimeMillis() - start;

        return LlmResponse.builder()
                .content(answer)
                .promptTokens(estimateTokens(request.getSystemPrompt()) + estimateTokens(request.getUserPrompt()))
                .completionTokens(estimateTokens(answer))
                .latencyMs(latency)
                .modelName(MODEL_NAME)
                .build();
    }

    @Override
    public String getModelIdentifier() {
        return MODEL_NAME;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return (int) Math.ceil(text.split("\\s+").length * 1.33);
    }
}
