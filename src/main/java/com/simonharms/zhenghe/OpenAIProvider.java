package com.simonharms.zhenghe;

/**
 * Provider for the <a href="https://platform.openai.com/">OpenAI</a> API.
 *
 * <p>Connects to OpenAI's chat completions endpoint. Supports all OpenAI models
 * including GPT-4o, GPT-4o-mini, o1, o3, etc.
 *
 * <p>Requires the {@code OPENAI_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class OpenAIProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.openai.com/v1";

    public OpenAIProvider(String apiKey) {
        super("openai", apiKey, BASE_URL);
    }

    public OpenAIProvider(String apiKey, String baseUrl) {
        super("openai", apiKey, baseUrl);
    }
}
