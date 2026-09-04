package com.rj.ReguLens.generation.verifier;

import com.rj.ReguLens.generation.model.AttributedClaim;
import com.rj.ReguLens.generation.model.GroundednessStatus;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class GroundednessVerifier {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[Ref:\\s*([a-f0-9\\-]+)\\]", Pattern.CASE_INSENSITIVE);
    private static final double MIN_ENTAILEMENT_THRESHOLD = 0.80;

    public VerificationResult verify(String answerText, List<ScoredDocumentChunk> retrievedChunks) {
        if (answerText == null || answerText.isBlank() || retrievedChunks == null || retrievedChunks.isEmpty()) {
            return new VerificationResult(List.of(), List.of(), GroundednessStatus.MISSING_CONTEXT, 0.0);
        }

        Map<UUID, ScoredDocumentChunk> chunkMap = new HashMap<>();
        for (ScoredDocumentChunk c : retrievedChunks) {
            if (c.getChunkId() != null) {
                chunkMap.put(c.getChunkId(), c);
            }
        }

        // Split answer into individual sentences
        String[] sentences = answerText.split("(?<=[.!?])\\s+");
        List<AttributedClaim> claims = new ArrayList<>();
        Set<UUID> allCitedIds = new LinkedHashSet<>();

        int totalClaimSentences = 0;
        int verifiedClaimSentences = 0;

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("⚠️ Regulatory Notice:")) {
                continue;
            }

            Matcher matcher = CITATION_PATTERN.matcher(trimmed);
            List<UUID> citedInSentence = new ArrayList<>();

            while (matcher.find()) {
                try {
                    UUID id = UUID.fromString(matcher.group(1));
                    citedInSentence.add(id);
                    allCitedIds.add(id);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID in citation tag: {}", matcher.group(1));
                }
            }

            // Remove citation tags to get clean assertion statement
            String cleanStatement = trimmed.replaceAll("\\[Ref:\\s*[a-f0-9\\-]+\\]", "").trim();
            if (cleanStatement.length() < 10) continue;

            totalClaimSentences++;

            if (citedInSentence.isEmpty()) {
                // Sentence made a claim with NO citation
                claims.add(AttributedClaim.builder()
                        .statement(cleanStatement)
                        .citedChunkId(null)
                        .clauseReference(null)
                        .verified(false)
                        .verificationReason("Claim lacks required source chunk citation.")
                        .build());
            } else {
                for (UUID chunkId : citedInSentence) {
                    ScoredDocumentChunk chunk = chunkMap.get(chunkId);
                    if (chunk == null) {
                        claims.add(AttributedClaim.builder()
                                .statement(cleanStatement)
                                .citedChunkId(chunkId)
                                .clauseReference(null)
                                .verified(false)
                                .verificationReason("Cited chunk ID not found in retrieved context.")
                                .build());
                    } else {
                        boolean entailed = verifyEntailment(cleanStatement, chunk.getContent());
                        if (entailed) {
                            verifiedClaimSentences++;
                        }
                        claims.add(AttributedClaim.builder()
                                .statement(cleanStatement)
                                .citedChunkId(chunkId)
                                .clauseReference(chunk.getClauseReference())
                                .verified(entailed)
                                .verificationReason(entailed ? "Entailment verified against source clause." : "Statement keywords not sufficiently supported by cited clause.")
                                .build());
                    }
                }
            }
        }

        double confidence = totalClaimSentences > 0 ? (double) verifiedClaimSentences / totalClaimSentences : 1.0;
        GroundednessStatus status = (confidence >= MIN_ENTAILEMENT_THRESHOLD) 
                ? GroundednessStatus.VERIFIED_ENTAILED 
                : GroundednessStatus.VERIFICATION_FAILED;

        log.info("Groundedness verification completed: {}/{} claims verified, confidence={}, status={}",
                verifiedClaimSentences, totalClaimSentences, confidence, status);

        return new VerificationResult(claims, new ArrayList<>(allCitedIds), status, confidence);
    }

    private boolean verifyEntailment(String statement, String chunkContent) {
        if (chunkContent == null || statement == null) return false;

        String lowerChunk = chunkContent.toLowerCase();
        String[] words = statement.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");

        int significantWords = 0;
        int matchedWords = 0;

        for (String word : words) {
            if (isStopWord(word) || word.length() < 3) continue;
            significantWords++;
            if (lowerChunk.contains(word)) {
                matchedWords++;
            }
        }

        if (significantWords == 0) return true;
        // Overlap ratio should exceed 45% of significant key terminology
        return ((double) matchedWords / significantWords) >= 0.45;
    }

    private boolean isStopWord(String word) {
        return Set.of(
                "the", "and", "is", "are", "to", "in", "that", "it", "with", "as",
                "for", "on", "was", "by", "an", "be", "this", "which", "or", "from",
                "at", "shall", "must", "all", "any", "accordance", "relevant", "applicable"
        ).contains(word);
    }

    @Value
    public static class VerificationResult {
        List<AttributedClaim> claims;
        List<UUID> citedChunkIds;
        GroundednessStatus status;
        double confidence;
    }
}
