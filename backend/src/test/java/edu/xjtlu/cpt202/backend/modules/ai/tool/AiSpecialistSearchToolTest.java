package edu.xjtlu.cpt202.backend.modules.ai.tool;

import edu.xjtlu.cpt202.backend.modules.ai.model.vo.AiSpecialistSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSpecialistSearchService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSpecialistSearchToolTest {

    @Test
    void shouldDelegateToSpecialistSearchService() {
        AiSpecialistSearchService service = mock(AiSpecialistSearchService.class);
        AiSpecialistSearchTool tool = new AiSpecialistSearchTool(service);
        AiSpecialistSearchResultVO expected = AiSpecialistSearchResultVO.builder()
                .query("heart beating fast")
                .returnedCount(1)
                .build();
        when(service.search("heart beating fast", "Cardiology", "CHIEF", "Dr. Li")).thenReturn(expected);

        AiSpecialistSearchResultVO actual = tool.searchSpecialists(
                "heart beating fast",
                "Cardiology",
                "CHIEF",
                "Dr. Li"
        );

        assertSame(expected, actual);
        verify(service).search("heart beating fast", "Cardiology", "CHIEF", "Dr. Li");
    }
}
