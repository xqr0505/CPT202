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
            Recent chat memory:
            {{memoryContext}}

            Current actionable booking candidates:
            {{candidateSummary}}

            RULES:
            1. Use the recent memory context to resolve vague references like "this", "that booking", or omitted subjects.
            2. You may only resolve to a booking that appears in the actionable candidates.
            3. If user input is unrelated or user clearly wants to quit this flow, return ACTION: ABORT.
            4. If there is enough info to identify exactly one candidate booking, return ACTION: RESOLVED_BOOKING_ID and BOOKING_ID.
            5. If there is not enough info, return ACTION: INSUFFICIENT_INFO.
            6. If there are still multiple plausible candidates, return ACTION: NEEDS_USER_ID_SELECTION.
            7. Optionally provide search hints using these fields when helpful:
               EXPERT_NAME, CATEGORY_NAME, STATUS, START_DATE, END_DATE, TIME_RANGE_TYPE.
            8. Return plain text only with one field per line in this exact style:
               ACTION: ...
               BOOKING_ID: ...
               EXPERT_NAME: ...
               CATEGORY_NAME: ...
               STATUS: ...
               START_DATE: ...
               END_DATE: ...
               TIME_RANGE_TYPE: ...
            9. Never leave a field blank. If a field is unknown or unused, output N/A.
            10. All inferred wording and intent handling must remain English-compatible.
            """)
    String process(
            @UserMessage String userMsg,
            @V("taskState") String taskState,
            @V("memoryContext") String memoryContext,
            @V("candidateSummary") String candidateSummary
    );
}
