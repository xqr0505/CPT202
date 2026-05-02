package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.service.KnowledgeQueryRewriteService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tools for retrieving platform knowledge from the RAG index.
 *
 * @author QiranXiao
 * @since 2026/4/21
 */
@Component
@ConditionalOnBean(ContentRetriever.class)
public class KnowledgeTools {

    private final ContentRetriever contentRetriever;
    private final KnowledgeQueryRewriteService queryRewriteService;

    public KnowledgeTools(
            ContentRetriever contentRetriever,
            KnowledgeQueryRewriteService queryRewriteService
    ) {
        this.contentRetriever = contentRetriever;
        this.queryRewriteService = queryRewriteService;
    }

    @Tool("Search the platform's knowledge base for policies, guides.")
    public String searchKnowledgeBase(
            @P("A concise natural-language search query about ExpertLink platform policies, guides, booking, cancellation, refund, or reschedule rules.") String query
    ) {
        String normalizedQuery = query == null ? "" : query.trim();
        List<Content> contents = retrieveWithRewriteFallback(normalizedQuery);
        if (contents.isEmpty()) {
            return AiConstant.KNOWLEDGE_NOT_FOUND_FALLBACK_MESSAGE;
        }

        String response = contents.stream()
                .map(Content::textSegment)
                .map(this::formatSegment)
                .collect(Collectors.joining("\n---\n"));
        return response;
    }

    private List<Content> retrieveWithRewriteFallback(String query) {
        if (query.isBlank()) {
            return List.of();
        }
        List<String> rewrittenQueries = queryRewriteService.rewrite(query);
        if (rewrittenQueries.isEmpty()) {
            return contentRetriever.retrieve(new Query(query));
        }

        LinkedHashSet<String> seenSegments = new LinkedHashSet<>();
        List<Content> merged = new ArrayList<>();
        for (String rewrittenQuery : rewrittenQueries) {
            List<Content> retrieved = contentRetriever.retrieve(new Query(rewrittenQuery));
            for (Content content : retrieved) {
                TextSegment segment = content.textSegment();
                String dedupeKey = segment == null ? content.toString() : segment.text() + "|" + segment.metadata();
                if (seenSegments.add(dedupeKey)) {
                    merged.add(content);
                }
            }
        }
        return merged;
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
