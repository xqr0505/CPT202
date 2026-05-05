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
            4. On every turn, distinguish whether each detected date/time expression refers to:
               - which existing booking the user means, or
               - what new date/time the user wants after rescheduling.
            5. If a bare time phrase like "today", "tomorrow", or "next Friday" could reasonably describe which booking the user means, prefer using it for booking lookup first.
            6. Only fill TARGET_DATE, TARGET_TIME, or TIME_HINT when you judge the user is describing the desired new reschedule time.
            7. Use START_DATE, END_DATE, and TIME_RANGE_TYPE only for identifying the existing booking.
            8. If there is enough info to identify exactly one candidate booking, return ACTION: RESOLVED_BOOKING_ID and BOOKING_ID.
            9. If there is not enough info, return ACTION: INSUFFICIENT_INFO.
            10. If there are still multiple plausible candidates, return ACTION: NEEDS_USER_ID_SELECTION.
            11. Optionally provide search hints using these fields when helpful:
               EXPERT_NAME, CATEGORY_NAME, STATUS, START_DATE, END_DATE, TIME_RANGE_TYPE.
            12. Example:
                - "I want to reschedule my booking today" -> TIME_RANGE_TYPE: TODAY, TARGET_DATE: N/A
                - "I want to reschedule booking 14 to tomorrow at 3pm" -> BOOKING_ID: 14, TARGET_DATE set, TARGET_TIME set
                - "I want to move tomorrow's booking to next Friday" -> use tomorrow for booking lookup and next Friday for TARGET_DATE
            13. Return plain text only with one field per line in this exact style:
               ACTION: ...
               BOOKING_ID: ...
               EXPERT_NAME: ...
               CATEGORY_NAME: ...
               STATUS: ...
               START_DATE: ...
               END_DATE: ...
               TIME_RANGE_TYPE: ...
               TARGET_DATE: ...
               TARGET_TIME: ...
               TIME_HINT: ...
            14. TARGET_DATE must be ISO yyyy-MM-dd when confidently known, otherwise N/A.
            15. TARGET_TIME should be a normalized 24-hour HH:mm when confidently known, otherwise N/A.
            16. TIME_HINT can be broader wording like morning, afternoon, evening, around 3pm, next Friday afternoon if helpful.
            17. Never leave a field blank. If a field is unknown or unused, output N/A.
            18. All inferred wording and intent handling must remain English-compatible.
            """)
    String process(
            @UserMessage String userMsg,
            @V("taskState") String taskState,
            @V("memoryContext") String memoryContext,
            @V("candidateSummary") String candidateSummary
    );
}
