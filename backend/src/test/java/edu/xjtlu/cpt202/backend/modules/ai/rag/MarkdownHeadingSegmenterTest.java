package edu.xjtlu.cpt202.backend.modules.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownHeadingSegmenterTest {

    private final MarkdownHeadingSegmenter segmenter = new MarkdownHeadingSegmenter();

    @Test
    void shouldSplitMarkdownByHeadingAndAttachMetadata() {
        String markdown = """
                ## Booking Process Guide

                Intro.

                ### Cancelling or Rescheduling

                Cancel and reschedule policy.
                """;

        MarkdownHeadingSegmenter.DocumentMetadata metadata = segmenter.buildMetadata(
                "knowledge/Booking_Process_Guide.md",
                markdown
        );

        List<TextSegment> segments = segmenter.split(markdown, metadata);

        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).metadata("documentId")).isEqualTo("booking-process-guide");
        assertThat(segments.get(0).metadata("source")).isEqualTo("knowledge/Booking_Process_Guide.md");
        assertThat(segments.get(0).metadata("documentType")).isEqualTo("booking_guide");
        assertThat(segments.get(0).metadata("title")).isEqualTo("Booking Process Guide");
        assertThat(segments.get(0).metadata("headingPath")).isEqualTo("Booking Process Guide");
        assertThat(segments.get(0).metadata("chunkIndex")).isEqualTo("0");
        assertThat(segments.get(0).metadata("ingestedAt")).isNotBlank();
        assertThat(segments.get(1).metadata("headingPath"))
                .isEqualTo("Booking Process Guide > Cancelling or Rescheduling");
    }
}
