package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.xjtlu.cpt202.backend.modules.ai.model.vo.AiSpecialistSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSpecialistSearchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AI tool for hybrid specialist retrieval.
 *
 * @author QiranXiao
 * @since 2026/5/7
 */
@Component
@ConditionalOnProperty(prefix = "ai.search.specialist", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiSpecialistSearchTool {

    private final AiSpecialistSearchService aiSpecialistSearchService;

    public AiSpecialistSearchTool(AiSpecialistSearchService aiSpecialistSearchService) {
        this.aiSpecialistSearchService = aiSpecialistSearchService;
    }

    @Tool("""
            Search medical specialists using hard filters, keyword matching, and semantic search.
            Use this when the user wants the most suitable doctor by symptom, condition, category, level, or name.
            If the user explicitly mentions a category, level, or specialist name, fill the filter arguments instead of only putting everything into searchQuery.
            This tool only finds matching specialists. If the user then wants available dates or time slots, call the specialist availability tool next using specialistId.
            """)
    public AiSpecialistSearchResultVO searchSpecialists(
            @P("The user's natural-language symptom description or specialist request. Required.") String searchQuery,
            @P("Explicit category filter such as Pediatrics, Cardiology, Psychiatry, or Gynecology. Optional.") String categoryFilter,
            @P("Explicit specialist level such as CHIEF, SENIOR, INTERMEDIATE, or JUNIOR. Optional.") String levelFilter,
            @P("Explicit specialist name if the user mentions one, for example Emily Chen. Optional.") String nameFilter
    ) {
        return aiSpecialistSearchService.search(searchQuery, categoryFilter, levelFilter, nameFilter);
    }
}
