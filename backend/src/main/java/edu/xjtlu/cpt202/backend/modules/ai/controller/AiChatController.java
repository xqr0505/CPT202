package edu.xjtlu.cpt202.backend.modules.ai.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.model.dto.ChatRequestDTO;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(AiConstant.API_V1_AI)
public class AiChatController {

    private final Assistant assistant;

    public AiChatController(Assistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping(AiConstant.CHAT_PATH)
    public Result<String> chat(@Valid @RequestBody ChatRequestDTO chatRequestDTO) {
        String reply = assistant.chat(chatRequestDTO.getMessage());
        return Result.success(reply);
    }
}
