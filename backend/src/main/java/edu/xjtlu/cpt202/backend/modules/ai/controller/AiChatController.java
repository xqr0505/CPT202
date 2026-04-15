package edu.xjtlu.cpt202.backend.modules.ai.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.model.dto.ChatRequestDTO;
import edu.xjtlu.cpt202.backend.modules.ai.model.vo.ChatStreamVO;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
/**
 * @author QiranXiao
 * @since 2026/4/15
 */
@RestController
@Validated
@RequestMapping(AiConstant.API_V1_AI)
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PreAuthorize(AiConstant.AI_CHAT_ACCESS_EXPRESSION)
    @PostMapping(AiConstant.CHAT_SYNC_PATH)
    public Result<String> chatSync(@Valid @RequestBody ChatRequestDTO chatRequestDTO) {
        String reply = aiChatService.chat(chatRequestDTO.getMessage());
        return Result.success(reply);
    }

    @PreAuthorize(AiConstant.AI_CHAT_ACCESS_EXPRESSION)
    @PostMapping(value = AiConstant.CHAT_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequestDTO chatRequestDTO) {
        SseEmitter emitter = new SseEmitter(AiConstant.SSE_TIMEOUT_MILLIS);
        aiChatService.streamChat(chatRequestDTO.getMessage())
                .onNext(token -> sendChunkEvent(emitter, token, Boolean.FALSE))
                .onComplete(chatResponse -> {
                    sendDoneEvent(emitter);
                    emitter.complete();
                })
                .onError(emitter::completeWithError)
                .start();
        return emitter;
    }

    @PreAuthorize(AiConstant.AI_CHAT_ACCESS_EXPRESSION)
    @DeleteMapping(AiConstant.CHAT_MEMORY_PATH)
    public Result<Void> clearMemory() {
        aiChatService.clearCurrentUserMemory();
        return Result.success();
    }

    private void sendChunkEvent(SseEmitter emitter, String token, Boolean done) {
        ChatStreamVO streamVO = ChatStreamVO.builder()
                .content(token)
                .done(done)
                .build();
        Result<ChatStreamVO> payload = Result.success(streamVO);
        try {
            emitter.send(SseEmitter.event()
                    .name(AiConstant.CHAT_STREAM_EVENT)
                    .data(payload));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void sendDoneEvent(SseEmitter emitter) {
        ChatStreamVO streamVO = ChatStreamVO.builder()
                .content(AiConstant.EMPTY_CONTENT)
                .done(Boolean.TRUE)
                .build();
        Result<ChatStreamVO> payload = Result.success(streamVO);
        try {
            emitter.send(SseEmitter.event()
                    .name(AiConstant.CHAT_STREAM_DONE_EVENT)
                    .data(payload));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
