package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Workflow-related AI settings.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.workflow")
public class AiWorkflowProperties {

    @NotBlank
    private String cancelKeyPrefix = "expertlink:ai:workflow:cancel";

    @Min(60)
    private Long cancelTtlSeconds = 1800L;

    @NotBlank
    private String rescheduleKeyPrefix = "expertlink:ai:workflow:reschedule";

    @Min(60)
    private Long rescheduleTtlSeconds = 1800L;

    @NotBlank
    private String bookingKeyPrefix = "expertlink:ai:workflow:booking";

    @Min(60)
    private Long bookingTtlSeconds = 1800L;
}
