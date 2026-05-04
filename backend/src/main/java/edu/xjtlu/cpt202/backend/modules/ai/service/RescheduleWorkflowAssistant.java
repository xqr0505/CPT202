package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Task-specific assistant used only inside the reschedule workflow.
 * @author QiranXiao
 * @since 2026/5/4
 */
public interface RescheduleWorkflowAssistant {

    @SystemMessage("""
            You are assisting the user to reschedule a booking.
            Your current task state: {{taskState}}.

            RULES:
            1. Stay focused on rescheduling and keep replies concise.
            2. If user input is unrelated to rescheduling, or user clearly wants to quit this flow,
               start your response with '[RESCHEDULE_TASK_ABORTED]'.
            3. Never answer unrelated questions in this workflow. Abort instead.
            4. All follow-up wording must be in English.
            """)
    String process(@UserMessage String userMsg, @V("taskState") String taskState);
}
