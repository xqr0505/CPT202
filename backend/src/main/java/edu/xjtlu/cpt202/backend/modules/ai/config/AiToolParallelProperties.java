package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parallel execution settings for read-only AI tools.
 *
 * @author QiranXiao
 * @since 2026/4/23
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.tools.parallel")
public class AiToolParallelProperties {

    private boolean enabled = true;

    @Min(1)
    private Integer maxConcurrency = 4;

    @Min(1)
    private Long timeoutMs = 60000L;

    private Set<String> readOnlyNames = new LinkedHashSet<>(Set.of(
            "searchCurrentCustomerBookings",
            "searchKnowledgeBase"
    ));
}
