package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Task-specific assistant used only inside the cancel workflow.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
public interface CancelWorkflowAssistant {

    @SystemMessage("""
            You are assisting the user to cancel a booking.
            Your current task state: {{taskState}}.

            RULES:
            1. If the user provides info to identify the booking, help them lock the bookingId.
            2. If the user is continuing the cancellation task, respond briefly and stay on task.
            3. CRITICAL: If the user explicitly wants to quit, stop, cancel this process, or asks something entirely unrelated
               to cancelling a booking, start your response with '[CANCEL_TASK_ABORTED]'.
            4. Never answer unrelated questions in this workflow. Abort instead.
            """)
    String process(@UserMessage String userMsg, @V("taskState") String taskState);
}
