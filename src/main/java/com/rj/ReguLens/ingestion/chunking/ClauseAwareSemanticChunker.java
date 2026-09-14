package com.rj.ReguLens.ingestion.chunking;

import com.rj.ReguLens.ingestion.model.ChunkMetadata;
import com.rj.ReguLens.ingestion.model.ExtractedClauseChunk;
import com.rj.ReguLens.ingestion.model.RawDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ClauseAwareSemanticChunker {

    private static final int DEFAULT_MAX_TOKENS_PER_CHUNK = 512;
    private static final int DEFAULT_OVERLAP_TOKENS = 64;
    private static final int MIN_TOKENS_PER_CHUNK = 40;

    // Regex for structural hierarchy
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^(?:#{1,2}\\s+)?(?:PART|Part|CHAPTER|Chapter|TITLE|Title|ANNEX|Annex)\\s+([IVXLCDM\\d]+|[A-Z])(?:[:\\s–—\\-]+\\s*(.+))?$",
            Pattern.MULTILINE
    );

    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
            "^(?:#{2,3}\\s+)?(?:ARTICLE|Article|SECTION|Section|RULE|Rule|CLAUSE|Clause)\\s+(\\d+[a-zA-Z]?(?:\\.\\d+)*)(?:[:\\s–—\\-\\.]+\\s*(.+))?$",
            Pattern.MULTILINE
    );

    private static final Pattern NUMBERED_SECTION_PATTERN = Pattern.compile(
            "^(?:#{2,4}\\s+)?(\\d+\\.\\d+(?:\\.\\d+)?)\\s+([A-Z][^\\n\\r]+)$",
            Pattern.MULTILINE
    );

    private static final Pattern SUBSECTION_PATTERN = Pattern.compile(
            "^(?:\\((\\d+|[a-zA-Z]|[ivxlcdmIVXLCDM]+)\\)|(\\d+|[a-zA-Z])\\.)\\s+(.+)$",
            Pattern.MULTILINE
    );

    public List<ExtractedClauseChunk> chunkDocument(RawDocument document) {
        return chunkDocument(document, DEFAULT_MAX_TOKENS_PER_CHUNK, DEFAULT_OVERLAP_TOKENS);
    }

    public List<ExtractedClauseChunk> chunkDocument(RawDocument document, int maxTokens, int overlapTokens) {
        if (document.getContent() == null || document.getContent().isBlank()) {
            return List.of();
        }

        String checksum = calculateSha256(document.getContent());
        String regName = document.getRegulationName() != null ? document.getRegulationName() : (document.getTitle() != null ? document.getTitle() : "Regulatory Policy");
        String jurisdiction = document.getJurisdiction() != null ? document.getJurisdiction() : "GLOBAL";

        List<RawStructuralBlock> blocks = parseStructuralBlocks(document.getContent(), regName);
        List<ExtractedClauseChunk> resultChunks = new ArrayList<>();
        int chunkIndex = 0;

        for (RawStructuralBlock block : blocks) {
            int blockTokens = estimateTokenCount(block.content);

            if (blockTokens <= maxTokens) {
                chunkIndex++;
                resultChunks.add(createChunk(
                        document,
                        chunkIndex,
                        block.clauseReference,
                        block.hierarchyPath,
                        block.content,
                        checksum
                ));
            } else {
                // Split large clause into smaller overlapping windows while preserving breadcrumbs
                List<String> subSegments = splitIntoOverlappingWindows(block.content, maxTokens, overlapTokens);
                int totalParts = subSegments.size();
                for (int part = 0; part < totalParts; part++) {
                    chunkIndex++;
                    String partSuffix = totalParts > 1 ? " [Part " + (part + 1) + "/" + totalParts + "]" : "";
                    String clauseRef = block.clauseReference + partSuffix;
                    resultChunks.add(createChunk(
                            document,
                            chunkIndex,
                            clauseRef,
                            block.hierarchyPath,
                            subSegments.get(part),
                            checksum
                    ));
                }
            }
        }

        // Merge isolated tiny chunks (< MIN_TOKENS_PER_CHUNK) with adjacent chunk if same parent section
        return mergeSmallChunks(resultChunks, maxTokens);
    }

    private List<RawStructuralBlock> parseStructuralBlocks(String fullText, String defaultRegName) {
        String[] lines = fullText.split("\\r?\\n");
        List<RawStructuralBlock> blocks = new ArrayList<>();

        String currentChapter = "";
        String currentArticle = "";
        String currentClause = "";
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                currentContent.append("\n");
                continue;
            }

            // Check for Chapter/Part header
            Matcher chapterMatcher = CHAPTER_PATTERN.matcher(trimmed);
            if (chapterMatcher.matches()) {
                if (currentContent.length() > 0) {
                    blocks.add(buildRawBlock(defaultRegName, currentChapter, currentArticle, currentClause, currentContent.toString().trim()));
                    currentContent.setLength(0);
                }
                String chapNum = chapterMatcher.group(1);
                String chapTitle = chapterMatcher.group(2) != null ? chapterMatcher.group(2).trim() : "";
                currentChapter = "Chapter " + chapNum + (chapTitle.isEmpty() ? "" : " - " + chapTitle);
                currentArticle = "";
                currentClause = "";
                currentContent.append(trimmed).append("\n");
                continue;
            }

            // Check for Article/Section header
            Matcher articleMatcher = ARTICLE_PATTERN.matcher(trimmed);
            if (articleMatcher.matches()) {
                if (currentContent.length() > 0) {
                    blocks.add(buildRawBlock(defaultRegName, currentChapter, currentArticle, currentClause, currentContent.toString().trim()));
                    currentContent.setLength(0);
                }
                String artNum = articleMatcher.group(1);
                String artTitle = articleMatcher.group(2) != null ? articleMatcher.group(2).trim() : "";
                currentArticle = "Article " + artNum + (artTitle.isEmpty() ? "" : " - " + artTitle);
                currentClause = "";
                currentContent.append(trimmed).append("\n");
                continue;
            }

            // Check for Numbered section header (e.g. 3.2 Access Control Requirements)
            Matcher numSectionMatcher = NUMBERED_SECTION_PATTERN.matcher(trimmed);
            if (numSectionMatcher.matches()) {
                if (currentContent.length() > 0) {
                    blocks.add(buildRawBlock(defaultRegName, currentChapter, currentArticle, currentClause, currentContent.toString().trim()));
                    currentContent.setLength(0);
                }
                String secNum = numSectionMatcher.group(1);
                String secTitle = numSectionMatcher.group(2).trim();
                currentArticle = "Section " + secNum + " - " + secTitle;
                currentClause = "";
                currentContent.append(trimmed).append("\n");
                continue;
            }

            // Check for Subsection / Paragraph (e.g. (1), 1., (a))
            Matcher subMatcher = SUBSECTION_PATTERN.matcher(trimmed);
            if (subMatcher.matches()) {
                int contentTokens = estimateTokenCount(currentContent.toString());
                if (contentTokens >= MIN_TOKENS_PER_CHUNK && currentContent.length() > 0) {
                    blocks.add(buildRawBlock(defaultRegName, currentChapter, currentArticle, currentClause, currentContent.toString().trim()));
                    currentContent.setLength(0);
                }
                String subNum = subMatcher.group(1) != null ? subMatcher.group(1) : subMatcher.group(2);
                currentClause = "Para " + subNum;
                currentContent.append(trimmed).append("\n");
                continue;
            }

            currentContent.append(line).append("\n");
        }

        if (currentContent.length() > 0 && !currentContent.toString().isBlank()) {
            blocks.add(buildRawBlock(defaultRegName, currentChapter, currentArticle, currentClause, currentContent.toString().trim()));
        }

        // If no structural blocks were recognized (e.g. plain unstructured text), treat as generic text blocks
        if (blocks.isEmpty()) {
            blocks.add(buildRawBlock(defaultRegName, "", "Section 1", "General", fullText.trim()));
        }

        return blocks;
    }

    private RawStructuralBlock buildRawBlock(String regName, String chapter, String article, String clause, String content) {
        StringBuilder hierarchy = new StringBuilder(regName);
        if (!chapter.isEmpty()) {
            hierarchy.append(" > ").append(chapter);
        }
        if (!article.isEmpty()) {
            hierarchy.append(" > ").append(article);
        }

        String clauseRef;
        if (!article.isEmpty() && !clause.isEmpty()) {
            clauseRef = article.split(" - ")[0] + "(" + clause.replace("Para ", "") + ")";
        } else if (!article.isEmpty()) {
            clauseRef = article;
        } else if (!chapter.isEmpty()) {
            clauseRef = chapter;
        } else {
            clauseRef = regName + " (Main)";
        }

        return new RawStructuralBlock(hierarchy.toString(), clauseRef, content);
    }

    private List<String> splitIntoOverlappingWindows(String text, int maxTokens, int overlapTokens) {
        List<String> windows = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?\\n])\\s+");

        StringBuilder currentWindow = new StringBuilder();
        int currentTokens = 0;
        List<String> overlapBuffer = new ArrayList<>();
        int overlapBufferTokens = 0;

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) continue;

            int sentenceTokens = estimateTokenCount(trimmedSentence);

            if (currentTokens + sentenceTokens > maxTokens && currentWindow.length() > 0) {
                windows.add(currentWindow.toString().trim());
                currentWindow.setLength(0);
                currentTokens = 0;

                // Re-inject overlap from the end of previous window
                for (String prevSentence : overlapBuffer) {
                    currentWindow.append(prevSentence).append(" ");
                    currentTokens += estimateTokenCount(prevSentence);
                }
                overlapBuffer.clear();
                overlapBufferTokens = 0;
            }

            currentWindow.append(trimmedSentence).append(" ");
            currentTokens += sentenceTokens;

            overlapBuffer.add(trimmedSentence);
            overlapBufferTokens += sentenceTokens;
            while (overlapBufferTokens > overlapTokens && overlapBuffer.size() > 1) {
                String removed = overlapBuffer.remove(0);
                overlapBufferTokens -= estimateTokenCount(removed);
            }
        }

        if (currentWindow.length() > 0 && !currentWindow.toString().isBlank()) {
            windows.add(currentWindow.toString().trim());
        }

        return windows.isEmpty() ? List.of(text) : windows;
    }

    private ExtractedClauseChunk createChunk(
            RawDocument document,
            int chunkIndex,
            String clauseReference,
            String sectionHierarchy,
            String rawContent,
            String checksumSha256
    ) {
        String regName = document.getRegulationName() != null ? document.getRegulationName() : (document.getTitle() != null ? document.getTitle() : "Regulatory Policy");
        String jurisdiction = document.getJurisdiction() != null ? document.getJurisdiction() : "GLOBAL";

        // Build enriched breadcrumb prefix
        String enrichedContent = String.format(
                "[DOCUMENT: %s]\n[JURISDICTION: %s]\n[HIERARCHY: %s]\n[CLAUSE: %s]\n\n%s",
                regName,
                jurisdiction,
                sectionHierarchy,
                clauseReference,
                rawContent
        );

        int tokenCount = estimateTokenCount(enrichedContent);

        ChunkMetadata metadata = ChunkMetadata.builder()
                .jurisdiction(jurisdiction)
                .regulationName(regName)
                .versionTag(document.getVersionTag() != null ? document.getVersionTag() : "v1.0")
                .effectiveDate(document.getEffectiveDate())
                .sectionHierarchy(sectionHierarchy)
                .clauseReference(clauseReference)
                .chunkIndex(chunkIndex)
                .tokenCount(tokenCount)
                .sourceUrl(document.getSourceUrl())
                .checksumSha256(checksumSha256)
                .customAttributes(document.getAdditionalAttributes())
                .build();

        return ExtractedClauseChunk.builder()
                .chunkIndex(chunkIndex)
                .clauseReference(clauseReference)
                .sectionHierarchy(sectionHierarchy)
                .rawContent(rawContent)
                .enrichedContent(enrichedContent)
                .tokenCount(tokenCount)
                .metadata(metadata)
                .build();
    }

    private List<ExtractedClauseChunk> mergeSmallChunks(List<ExtractedClauseChunk> chunks, int maxTokens) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<ExtractedClauseChunk> merged = new ArrayList<>();
        ExtractedClauseChunk current = chunks.get(0);

        for (int i = 1; i < chunks.size(); i++) {
            ExtractedClauseChunk next = chunks.get(i);

            boolean sameHierarchy = current.getSectionHierarchy().equals(next.getSectionHierarchy());
            boolean combinedUnderBudget = (current.getTokenCount() + next.getTokenCount()) <= maxTokens;
            boolean currentTooSmall = current.getTokenCount() < MIN_TOKENS_PER_CHUNK;

            if (sameHierarchy && combinedUnderBudget && currentTooSmall) {
                // Merge current and next
                String combinedRaw = current.getRawContent() + "\n\n" + next.getRawContent();
                String combinedRef = current.getClauseReference() + " & " + next.getClauseReference();
                String combinedEnriched = String.format(
                        "[DOCUMENT: %s]\n[JURISDICTION: %s]\n[HIERARCHY: %s]\n[CLAUSE: %s]\n\n%s",
                        current.getMetadata().getRegulationName(),
                        current.getMetadata().getJurisdiction(),
                        current.getSectionHierarchy(),
                        combinedRef,
                        combinedRaw
                );
                int tokenCount = estimateTokenCount(combinedEnriched);

                ChunkMetadata combinedMeta = ChunkMetadata.builder()
                        .jurisdiction(current.getMetadata().getJurisdiction())
                        .regulationName(current.getMetadata().getRegulationName())
                        .versionTag(current.getMetadata().getVersionTag())
                        .effectiveDate(current.getMetadata().getEffectiveDate())
                        .sectionHierarchy(current.getSectionHierarchy())
                        .clauseReference(combinedRef)
                        .chunkIndex(current.getChunkIndex())
                        .tokenCount(tokenCount)
                        .sourceUrl(current.getMetadata().getSourceUrl())
                        .checksumSha256(current.getMetadata().getChecksumSha256())
                        .customAttributes(current.getMetadata().getCustomAttributes())
                        .build();

                current = ExtractedClauseChunk.builder()
                        .chunkIndex(current.getChunkIndex())
                        .clauseReference(combinedRef)
                        .sectionHierarchy(current.getSectionHierarchy())
                        .rawContent(combinedRaw)
                        .enrichedContent(combinedEnriched)
                        .tokenCount(tokenCount)
                        .metadata(combinedMeta)
                        .build();
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        // Re-index sequentially
        List<ExtractedClauseChunk> reindexed = new ArrayList<>();
        for (int idx = 0; idx < merged.size(); idx++) {
            ExtractedClauseChunk c = merged.get(idx);
            ChunkMetadata meta = ChunkMetadata.builder()
                    .jurisdiction(c.getMetadata().getJurisdiction())
                    .regulationName(c.getMetadata().getRegulationName())
                    .versionTag(c.getMetadata().getVersionTag())
                    .effectiveDate(c.getMetadata().getEffectiveDate())
                    .sectionHierarchy(c.getSectionHierarchy())
                    .clauseReference(c.getClauseReference())
                    .chunkIndex(idx + 1)
                    .tokenCount(c.getTokenCount())
                    .sourceUrl(c.getMetadata().getSourceUrl())
                    .checksumSha256(c.getMetadata().getChecksumSha256())
                    .customAttributes(c.getMetadata().getCustomAttributes())
                    .build();

            reindexed.add(ExtractedClauseChunk.builder()
                    .chunkIndex(idx + 1)
                    .clauseReference(c.getClauseReference())
                    .sectionHierarchy(c.getSectionHierarchy())
                    .rawContent(c.getRawContent())
                    .enrichedContent(c.getEnrichedContent())
                    .tokenCount(c.getTokenCount())
                    .metadata(meta)
                    .build());
        }

        return reindexed;
    }

    public int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // Heuristic: words + punctuation count / 0.75 ratio
        String[] words = text.trim().split("\\s+");
        return (int) Math.ceil(words.length * 1.33);
    }

    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return Integer.toHexString(input.hashCode());
        }
    }

    private record RawStructuralBlock(
            String hierarchyPath,
            String clauseReference,
            String content
    ) {}
}
