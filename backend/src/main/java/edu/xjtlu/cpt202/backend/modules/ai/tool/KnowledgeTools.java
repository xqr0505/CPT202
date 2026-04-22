package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tools for retrieving platform knowledge from the RAG index.
 *
 * @author QiranXiao
 * @since 2026/4/21
 */
@Component
public class KnowledgeTools {

    private final ContentRetriever contentRetriever;

    public KnowledgeTools(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    @Tool("Search the platform's knowledge base for policies, guides, and medical department information.")
    public String searchKnowledgeBase(
            @P("A concise natural-language search query about ExpertLink platform policies, guides, booking, cancellation, refund, or reschedule rules.") String query
    ) {
        List<Content> contents = contentRetriever.retrieve(new Query(query));
        if (contents.isEmpty()) {
            return "No relevant knowledge base content was found.";
        }

        return contents.stream()
                .map(Content::textSegment)
                .map(this::formatSegment)
                .collect(Collectors.joining("\n---\n"));
    }

    private String formatSegment(TextSegment segment) {
        String source = segment.metadata("source");
        String headingPath = segment.metadata("headingPath");
        String chunkIndex = segment.metadata("chunkIndex");
        return "Source: " + emptyToUnknown(source)
                + "\nHeading: " + emptyToUnknown(headingPath)
                + "\nChunk: " + emptyToUnknown(chunkIndex)
                + "\nContent:\n" + segment.text();
    }

    private String emptyToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
