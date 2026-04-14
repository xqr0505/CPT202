package edu.xjtlu.cpt202.backend.modules.ai.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
@Data
@Schema(description = "AI chat request")
public class ChatRequestDTO {

    @NotBlank(message = "message cannot be blank")
    @Schema(description = "User message content", example = "How should I prepare for my first consultation?")
    private String message;
}
