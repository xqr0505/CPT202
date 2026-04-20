package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeToolsTest {

    private final ContentRetriever contentRetriever = mock(ContentRetriever.class);
    private final KnowledgeTools knowledgeTools = new KnowledgeTools(contentRetriever);

    @Test
    void shouldReturnMessageWhenNoKnowledgeFound() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of());

        String result = knowledgeTools.searchKnowledgeBase("refund");

        assertThat(result).isEqualTo("No relevant knowledge base content was found.");
    }

    @Test
    void shouldFormatRetrievedSegmentsWithMetadata() {
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
        when(contentRetriever.retrieve(any())).thenReturn(List.of(Content.from(first), Content.from(second)));

        String result = knowledgeTools.searchKnowledgeBase("refund");

        assertThat(result).contains("Source: knowledge/Booking_Process_Guide.md");
        assertThat(result).contains("Heading: Booking Process Guide > Cancelling or Rescheduling");
        assertThat(result).contains("Chunk: 1");
        assertThat(result).contains("full-refund window");
        assertThat(result).contains("\n---\n");
        assertThat(result).contains("within 2 hours");
    }
}
