package com.rj.ReguLens.generation.prompt;

import com.rj.ReguLens.generation.model.LlmRequest;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class RegulatoryPromptBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final String SYSTEM_PROMPT = """
        You are ReguLens, an audit-grade regulatory compliance intelligence engine.
        Your mission is to provide accurate, factual, and strictly verifiable compliance answers based EXCLUSIVELY on the provided regulatory policy text chunks.

        CRITICAL POLICY LIFECYCLE & GROUNDING RULES:
        1. ANSWER ACCURACY & GROUNDING: Base every assertion strictly on the provided Context Chunks. Do not use external knowledge or extrapolate.
        2. LATEST & ACTIVE POLICY PREFERENCE: Prioritize the most recent ACTIVE policy versions.
        3. EXPIRED POLICY HANDLING: If the only retrieved policy matching the query has status EXPIRED, you MUST begin your response with an explicit regulatory disclaimer:
           "⚠️ Regulatory Notice: Policy '[Policy Name]' (Version [Version]) expired on [Effective To Date]. As per this expired policy:"
           followed by the factual answer from that version.
        4. INSUFFICIENT / MISSING CONTEXT: If no context chunks are provided or the retrieved chunks do not contain sufficient facts to answer the question with certainty, reply:
           "The indexed regulatory policy documents do not contain sufficient context to answer this compliance question."
        5. INLINE CITATIONS: Every sentence containing a factual assertion MUST include an inline citation referencing the chunk ID: [Ref: <chunk_id>].
        6. OBJECTIVE TONE: Maintain a formal, regulatory-grade tone without providing speculative personal opinions.
        """;

    public LlmRequest buildRequest(String queryText, List<ScoredDocumentChunk> chunks, boolean onlyExpiredFound) {
        StringBuilder userPrompt = new StringBuilder();

        userPrompt.append("COMPLIANCE INQUIRY:\n").append(queryText).append("\n\n");

        if (onlyExpiredFound && !chunks.isEmpty()) {
            ScoredDocumentChunk topExpired = chunks.get(0);
            String expDate = topExpired.getEffectiveTo() != null ? topExpired.getEffectiveTo().format(DATE_FORMATTER) : "N/A";
            userPrompt.append("NOTICE TO MODEL: The only matching policy in the repository is EXPIRED. You MUST prepend your response with:\n")
                    .append("⚠️ Regulatory Notice: Policy '").append(topExpired.getPolicyTitle())
                    .append("' (Version ").append(topExpired.getVersionNumber())
                    .append(") expired on ").append(expDate).append(". As per this expired policy:\n\n");
        }

        userPrompt.append("CONTEXT CHUNKS (STRICT SOURCE MATERIAL):\n");

        for (int i = 0; i < chunks.size(); i++) {
            ScoredDocumentChunk chunk = chunks.get(i);
            userPrompt.append("--- CHUNK ").append(i + 1).append(" ---\n")
                    .append("[CHUNK_ID: ").append(chunk.getChunkId() != null ? chunk.getChunkId().toString() : "chunk-" + i).append("]\n")
                    .append("[POLICY: ").append(chunk.getPolicyTitle()).append(" v").append(chunk.getVersionNumber()).append("]\n")
                    .append("[STATUS: ").append(chunk.getPolicyStatus()).append("]\n")
                    .append("[CLAUSE_REF: ").append(chunk.getClauseReference()).append("]\n")
                    .append("[HIERARCHY: ").append(chunk.getSectionHierarchy()).append("]\n")
                    .append("[CONTENT]:\n").append(chunk.getContent()).append("\n\n");
        }

        return LlmRequest.builder()
                .systemPrompt(SYSTEM_PROMPT.trim())
                .userPrompt(userPrompt.toString().trim())
                .temperature(0.0)
                .maxTokens(1024)
                .build();
    }
}
