package com.rj.ReguLens.generation.adapter;

import com.rj.ReguLens.generation.LlmClientPort;
import com.rj.ReguLens.generation.model.LlmRequest;
import com.rj.ReguLens.generation.model.LlmResponse;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("groqChatAdapter")
@Primary
@ConditionalOnProperty(name = "regulens.ai.llm.provider", havingValue = "groq")
@Slf4j
public class GroqChatAdapter implements LlmClientPort {

    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";

    private final ChatModel chatModel;
    private final MockChatAdapter fallbackAdapter;
    private final String modelName;

    public GroqChatAdapter(
            @Value("${regulens.ai.llm.api-key:${GROQ_API_KEY:}}") String apiKey,
            @Value("${regulens.ai.llm.model-name:llama-3.3-70b-versatile}") String modelName,
            @Value("${regulens.ai.llm.temperature:0.0}") double temperature,
            MockChatAdapter fallbackAdapter
    ) {
        this.fallbackAdapter = fallbackAdapter;
        this.modelName = modelName != null && !modelName.isBlank() ? modelName : DEFAULT_MODEL;

        if (apiKey != null && !apiKey.isBlank() && !apiKey.equals("test-key")) {
            log.info("Initializing Groq ChatModel with base URL: {} and model: {}", GROQ_BASE_URL, this.modelName);
            this.chatModel = OpenAiChatModel.builder()
                    .baseUrl(GROQ_BASE_URL)
                    .apiKey(apiKey)
                    .modelName(this.modelName)
                    .temperature(temperature)
                    .build();
        } else {
            log.warn("Groq API key is not configured. Operating in fallback mock chat mode.");
            this.chatModel = null;
        }
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (chatModel == null) {
            return fallbackAdapter.generate(request);
        }

        long start = System.currentTimeMillis();
        List<ChatMessage> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        if (request.getUserPrompt() != null && !request.getUserPrompt().isBlank()) {
            messages.add(new UserMessage(request.getUserPrompt()));
        }

        try {
            ChatResponse response = chatModel.chat(messages);
            long latency = System.currentTimeMillis() - start;

            String text = response.aiMessage().text();
            int promptTokens = response.tokenUsage() != null ? response.tokenUsage().inputTokenCount() : 0;
            int completionTokens = response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : 0;

            return LlmResponse.builder()
                    .content(text)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .latencyMs(latency)
                    .modelName(modelName)
                    .build();
        } catch (Exception e) {
            log.error("Error during Groq LLM generation call, falling back to mock adapter: {}", e.getMessage());
            return fallbackAdapter.generate(request);
        }
    }

    @Override
    public String getModelIdentifier() {
        return "groq-" + modelName;
    }
}
