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
            Recent chat memory:
            {{memoryContext}}

            Current actionable booking candidates:
            {{candidateSummary}}

            RULES:
            1. Use the recent memory context to resolve vague references like "this", "that one", or omitted subjects.
            2. You may only resolve to a booking that appears in the actionable candidates.
            3. If the user explicitly wants to quit, stop, cancel this process, or is clearly unrelated, return ACTION: ABORT.
            4. If there is enough info to identify exactly one candidate booking, return ACTION: RESOLVED_BOOKING_ID and BOOKING_ID.
            5. If there is not enough info, return ACTION: INSUFFICIENT_INFO.
            6. If there are still multiple plausible candidates, return ACTION: NEEDS_USER_ID_SELECTION.
            7. Optionally provide search hints using these fields when helpful:
               EXPERT_NAME, CATEGORY_NAME, STATUS, START_DATE, END_DATE, TIME_RANGE_TYPE.
               If you provide START_DATE or END_DATE, prefer ISO format YYYY-MM-DD.
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
            """)
    String process(
            @UserMessage String userMsg,
            @V("taskState") String taskState,
            @V("memoryContext") String memoryContext,
            @V("candidateSummary") String candidateSummary
    );
}
