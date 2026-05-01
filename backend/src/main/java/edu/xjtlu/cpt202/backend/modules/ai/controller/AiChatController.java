package edu.xjtlu.cpt202.backend.modules.ai.controller;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.model.dto.ChatRequestDTO;
import edu.xjtlu.cpt202.backend.modules.ai.model.vo.ChatStreamVO;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatTraceContext;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@Validated
@RequestMapping(AiConstant.API_V1_AI)
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiChatProfiler aiChatProfiler;

    public AiChatController(AiChatService aiChatService, AiChatProfiler aiChatProfiler) {
        this.aiChatService = aiChatService;
        this.aiChatProfiler = aiChatProfiler;
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
        long requestStartNs = System.nanoTime();
        Long userId = SecurityUtils.getCurrentUserId();
        String traceId = aiChatProfiler.startTrace(
                "controller.chat.request",
                userId,
                preview(chatRequestDTO.getMessage())
        );
        SseEmitter emitter = new SseEmitter(AiConstant.SSE_TIMEOUT_MILLIS);
        try {
            aiChatProfiler.logStage("controller.chat.toTokenStream", elapsedMs(requestStartNs), java.util.Map.of(
                    "traceId", traceId,
                    "userId", userId
            ));
            long streamStartNs = System.nanoTime();
            aiChatService.streamChat(chatRequestDTO.getMessage())
                    .onNext(token -> {
                        AiChatTraceContext.restoreTraceId(traceId);
                        sendChunkEvent(emitter, token, Boolean.FALSE);
                    })
                    .onComplete(chatResponse -> {
                        AiChatTraceContext.restoreTraceId(traceId);
                        aiChatProfiler.logSummary("controller.chat.streamComplete", elapsedMs(requestStartNs), java.util.Map.of(
                                "userId", userId
                        ));
                        sendDoneEvent(emitter);
                        emitter.complete();
                        aiChatProfiler.clearTrace();
                    })
                    .onError(error -> {
                        AiChatTraceContext.restoreTraceId(traceId);
                        handleStreamError(emitter, error, requestStartNs, userId);
                    })
                    .start();
            aiChatProfiler.logStage("controller.chat.startInvoked", elapsedMs(streamStartNs), java.util.Map.of(
                    "userId", userId
            ));
        } catch (Exception exception) {
            handleStreamError(emitter, exception, requestStartNs, userId);
        }
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
        sendEvent(emitter, AiConstant.CHAT_STREAM_EVENT, payload);
    }

    private void sendDoneEvent(SseEmitter emitter) {
        ChatStreamVO streamVO = ChatStreamVO.builder()
                .content(AiConstant.EMPTY_CONTENT)
                .done(Boolean.TRUE)
                .build();
        Result<ChatStreamVO> payload = Result.success(streamVO);
        sendEvent(emitter, AiConstant.CHAT_STREAM_DONE_EVENT, payload);
    }

    private void handleStreamError(SseEmitter emitter, Throwable throwable, long requestStartNs, Long userId) {
        log.error("AI chat stream failed", throwable);
        aiChatProfiler.logSummary("controller.chat.streamError", elapsedMs(requestStartNs), java.util.Map.of(
                "userId", userId,
                "errorType", throwable.getClass().getSimpleName(),
                "errorMessage", throwable.getMessage()
        ));
        Result<ChatStreamVO> payload = toErrorPayload(throwable);
        sendEvent(emitter, AiConstant.CHAT_STREAM_DONE_EVENT, payload);
        emitter.complete();
        aiChatProfiler.clearTrace();
    }

    private Result<ChatStreamVO> toErrorPayload(Throwable throwable) {
        if (throwable instanceof BusinessException businessException) {
            return Result.fail(businessException.getCode(), businessException.getMessage());
        }
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR.getCode(), ResultCodeEnum.SYSTEM_ERROR.getMessage());
    }

    private void sendEvent(SseEmitter emitter, String eventName, Result<ChatStreamVO> payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload));
        } catch (IOException e) {
            emitter.complete();
        }
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 80 ? value : value.substring(0, 80) + "...(" + value.length() + ")";
    }
}
