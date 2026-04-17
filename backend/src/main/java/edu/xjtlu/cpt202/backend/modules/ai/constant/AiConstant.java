package edu.xjtlu.cpt202.backend.modules.ai.constant;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
public final class AiConstant {

    private AiConstant() {
    }

    public static final String API_V1_AI = "/api/v1/ai";
    public static final String CHAT_PATH = "/chat";
    public static final String CHAT_SYNC_PATH = "/chat/sync";
    public static final String CHAT_MEMORY_PATH = "/chat/memory";
    public static final String CHAT_STREAM_EVENT = "chat";
    public static final String CHAT_STREAM_DONE_EVENT = "done";
    public static final String EMPTY_CONTENT = "";
    public static final Long SSE_TIMEOUT_MILLIS = 0L;
    public static final String AI_CHAT_ACCESS_EXPRESSION = "hasRole('CUSTOMER')";

    public static final String AI_SYSTEM_PROMPT = """
            You are the official customer support assistant for ExpertLink, a professional medical appointment platform.
            Your responsibilities are strictly limited to platform support, including booking workflows, account access, order status,
            specialist profile visibility, schedule-related guidance, and payment process explanations.
            When a customer asks about their own bookings or appointments, use the available booking search tool to retrieve the current customer's booking data before answering.
            You must not provide medical diagnosis, treatment plans, medication advice, emergency judgment, or any disease-specific interpretation.
            If users request medical advice, politely refuse and guide them to consult a licensed doctor through the booking flow or emergency services.
            Keep your tone professional, empathetic, and concise.
            """;

    public static final String OPENAI_API_KEY_REQUIRED_MESSAGE =
            "AI config error: ai.openai.api-key is required (env: OPENAI_API_KEY).";
    public static final String OPENAI_MODEL_NAME_REQUIRED_MESSAGE =
            "AI config error: ai.openai.model-name is required (env: OPENAI_MODEL_NAME).";
}
