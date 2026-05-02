package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import edu.xjtlu.cpt202.backend.modules.ai.service.KnowledgeQueryRewriteService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeToolsTest {

    private final ContentRetriever contentRetriever = mock(ContentRetriever.class);
    private final KnowledgeQueryRewriteService queryRewriteService = mock(KnowledgeQueryRewriteService.class);
    private final AiSemanticCacheService semanticCacheService = mock(AiSemanticCacheService.class);
    private final KnowledgeTools knowledgeTools = new KnowledgeTools(contentRetriever, queryRewriteService, semanticCacheService);

    @Test
    void shouldReturnCachedKnowledgeWhenSemanticCacheHits() {
        when(semanticCacheService.get("refund policy", AiIntent.KNOWLEDGE))
                .thenReturn(Optional.of(new AiSemanticCacheService.CacheHit("cache-1", "cached answer", true, 1.0D)));

        String result = knowledgeTools.searchKnowledgeBase("refund policy");

        assertThat(result).isEqualTo("cached answer");
        verify(queryRewriteService, never()).rewrite(any());
        verify(contentRetriever, never()).retrieve(any());
    }

    @Test
    void shouldReturnMessageWhenNoKnowledgeFound() {
        when(semanticCacheService.get("refund", AiIntent.KNOWLEDGE)).thenReturn(Optional.empty());
        when(queryRewriteService.rewrite("refund")).thenReturn(List.of());
        when(contentRetriever.retrieve(any())).thenReturn(List.of());

        String result = knowledgeTools.searchKnowledgeBase("refund");

        assertThat(result).isEqualTo(AiConstant.KNOWLEDGE_NOT_FOUND_FALLBACK_MESSAGE);
    }

    @Test
    void shouldFormatRetrievedSegmentsWithMetadata() {
        when(semanticCacheService.get("refund", AiIntent.KNOWLEDGE)).thenReturn(Optional.empty());
        when(queryRewriteService.rewrite("refund")).thenReturn(List.of("refund policy", "money back", "cancel refund"));
        TextSegment first = TextSegment.from(
                "Cancel earlier than 24 hours before the start time to stay in the full-refund window.",
                Metadata.metadata("source", "knowledge/Booking_Process_Guide.md")
                        .put("headingPath", "Booking Process Guide > Cancelling or Rescheduling")
                        .put("chunkIndex", 1)
        );
        TextSegment second = TextSegment.from(
                "If booking start time is within 2 hours, cancel/reschedule is blocked.",
                Metadata.metadata("source", "knowledge/Booking_Process_Guide.md")
                        .put("headingPath", "Booking Process Guide > Cancelling or Rescheduling")
                        .put("chunkIndex", 2)
        );
        when(contentRetriever.retrieve(any()))
                .thenReturn(List.of(Content.from(first)))
                .thenReturn(List.of(Content.from(first), Content.from(second)))
                .thenReturn(List.of(Content.from(second)));

        String result = knowledgeTools.searchKnowledgeBase("refund");

        assertThat(result).contains("Source: knowledge/Booking_Process_Guide.md");
        assertThat(result).contains("Heading: Booking Process Guide > Cancelling or Rescheduling");
        assertThat(result).contains("Chunk: 1");
        assertThat(result).contains("full-refund window");
        assertThat(result).contains("\n---\n");
        assertThat(result).contains("within 2 hours");
        verify(contentRetriever, times(3)).retrieve(any());
        verify(semanticCacheService).putAsync(eq("refund"), any(), eq(AiIntent.KNOWLEDGE));
    }

    @Test
    void shouldFallbackToOriginalQueryWhenRewriteCannotProvideThreeQueries() {
        when(semanticCacheService.get("reschedule", AiIntent.KNOWLEDGE)).thenReturn(Optional.empty());
        when(queryRewriteService.rewrite("reschedule")).thenReturn(List.of());
        when(contentRetriever.retrieve(any())).thenReturn(List.of());

        knowledgeTools.searchKnowledgeBase("reschedule");

        verify(queryRewriteService).rewrite("reschedule");
        verify(contentRetriever, times(1)).retrieve(any());
    }
}
