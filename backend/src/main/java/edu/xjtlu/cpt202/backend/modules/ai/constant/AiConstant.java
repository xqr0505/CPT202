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
    public static final String KNOWLEDGE_NOT_FOUND_FALLBACK_MESSAGE =
            "I'm sorry, but I cannot find that information in my current guidelines. Please contact support at 2906326615@qq.com.";
    public static final String KNOWLEDGE_QUERY_REWRITE_PROMPT = """
            You are an expert search query generator for a medical booking platform.
            Your task is to analyze the user's input and generate 3 alternative search queries.
            Focus on extracting core entities, actions, and symptoms. Convert colloquial phrases into formal system terms (e.g., 'can't find' -> 'search empty', 'money back' -> 'refund', 'change time' -> 'reschedule').
            The output must be in English only.
            User Input: %s
            Output only the 3 queries separated by commas, no other text.
            """;

    public static final String AI_SYSTEM_PROMPT = """
            You are a professional customer support agent for ExpertLink.
            Style: Concise. No fluff.
            You are given [Context] excerpts from our official platform manual.
            Scope: platform support only (booking workflows, account access, order status, specialist profile visibility, schedule guidance, payment process explanations).

            Grounding and safety rules:
            - Use [Context] as primary source of truth. You may synthesize across snippets and make limited common-sense inferences only when directly grounded in [Context].
            - Do not invent or guess any platform-specific feature, UI element, filter, tool behavior, policy, status, fee, workflow step, or specialist availability.
            - If [Context] is insufficient for a platform-logic answer, reply exactly:
            "I'm sorry, but I cannot find that information in my current guidelines. Please contact support at 2906326615@qq.com."
            - If a multi-part question is only partially supported, answer only supported parts and explicitly say the rest is not in current guidelines.
            - If the question is outside platform support scope, reply briefly that your scope is ExpertLink platform support.
            - If knowledge base retrieval returns no relevant content, reply exactly:
            "I'm sorry, but I cannot find that information in my current guidelines. Please contact support at 2906326615@qq.com."
            - Do not provide medical diagnosis, treatment plans, medication advice, emergency judgement, or disease-specific interpretation; guide users to licensed doctors/emergency services when needed.
            """;

    public static final String OPENAI_API_KEY_REQUIRED_MESSAGE =
            "AI config error: ai.openai.api-key is required (env: OPENAI_API_KEY).";
    public static final String OPENAI_MODEL_NAME_REQUIRED_MESSAGE =
            "AI config error: ai.openai.model-name is required (env: OPENAI_MODEL_NAME).";
}
