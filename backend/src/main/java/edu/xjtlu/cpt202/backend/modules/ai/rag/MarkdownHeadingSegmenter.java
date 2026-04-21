package edu.xjtlu.cpt202.backend.modules.ai.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Splits Markdown documents by heading hierarchy.
 *
 * @author QiranXiao
 * @since 2026/4/21
 */
@Component
public class MarkdownHeadingSegmenter {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final int MAX_SEGMENT_CHARS = 1800;

    public List<TextSegment> split(String markdown, DocumentMetadata metadata) {
        List<Section> sections = splitIntoSections(markdown);
        List<TextSegment> segments = new ArrayList<>();
        int[] chunkIndex = {0};
        String ingestedAt = Instant.now().toString();

        for (Section section : sections) {
            splitOversizedSection(section).forEach(text -> {
                Metadata segmentMetadata = Metadata.metadata("documentId", metadata.documentId())
                        .put("source", metadata.source())
                        .put("documentType", metadata.documentType())
                        .put("title", metadata.title())
                        .put("headingPath", section.headingPath())
                        .put("chunkIndex", chunkIndex[0]++)
                        .put("ingestedAt", ingestedAt);
                segments.add(TextSegment.from(text, segmentMetadata));
            });
        }

        return segments;
    }

    private List<Section> splitIntoSections(String markdown) {
        String[] headings = new String[6];
        List<String> currentLines = new ArrayList<>();
        List<Section> sections = new ArrayList<>();
        String currentHeadingPath = "";

        for (String line : markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            Matcher matcher = HEADING_PATTERN.matcher(line);
            if (matcher.matches()) {
                flushSection(sections, currentLines, currentHeadingPath);
                int level = matcher.group(1).length();
                String heading = matcher.group(2).trim();
                headings[level - 1] = heading;
                Arrays.fill(headings, level, headings.length, null);
                currentHeadingPath = Arrays.stream(headings)
                        .filter(value -> value != null && !value.isBlank())
                        .collect(Collectors.joining(" > "));
            }
            currentLines.add(line);
        }

        flushSection(sections, currentLines, currentHeadingPath);
        return sections;
    }

    private void flushSection(List<Section> sections, List<String> lines, String headingPath) {
        String text = String.join("\n", lines).trim();
        if (!text.isBlank()) {
            sections.add(new Section(text, headingPath == null || headingPath.isBlank() ? "Document" : headingPath));
        }
        lines.clear();
    }

    private List<String> splitOversizedSection(Section section) {
        if (section.text().length() <= MAX_SEGMENT_CHARS) {
            return List.of(section.text());
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : section.text().split("\\n\\s*\\n")) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isBlank()) {
                continue;
            }
            if (current.length() > 0 && current.length() + trimmedParagraph.length() + 2 > MAX_SEGMENT_CHARS) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(trimmedParagraph);
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    public DocumentMetadata buildMetadata(String source, String markdown) {
        String fileName = source.substring(source.lastIndexOf('/') + 1);
        String baseName = fileName.replaceFirst("\\.[^.]+$", "");
        String title = extractTitle(markdown, baseName);
        String documentId = baseName
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-|-$", "")
                .toLowerCase(Locale.ROOT);
        String documentType = documentId.contains("booking") ? "booking_guide" : "knowledge_document";
        return new DocumentMetadata(documentId, source, documentType, title);
    }

    private String extractTitle(String markdown, String fallback) {
        for (String line : markdown.split("\\R")) {
            Matcher matcher = HEADING_PATTERN.matcher(line);
            if (matcher.matches() && matcher.group(1).length() == 2) {
                return matcher.group(2).trim();
            }
            if (matcher.matches() && matcher.group(1).length() == 1) {
                return matcher.group(2).trim();
            }
        }
        return fallback;
    }

    public record DocumentMetadata(String documentId, String source, String documentType, String title) {
    }

    private record Section(String text, String headingPath) {
    }
}
