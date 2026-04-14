package edu.xjtlu.cpt202.backend.modules.ai.constant;

/**
 * Constants for AI module.
 */
public final class AiConstant {

    private AiConstant() {
    }

    public static final String API_V1_AI = "/api/v1/ai";
    public static final String CHAT_PATH = "/chat";

    public static final String OPENAI_API_KEY_REQUIRED_MESSAGE =
            "AI config error: ai.openai.api-key is required (env: OPENAI_API_KEY).";
    public static final String OPENAI_MODEL_NAME_REQUIRED_MESSAGE =
            "AI config error: ai.openai.model-name is required (env: OPENAI_MODEL_NAME).";
}
