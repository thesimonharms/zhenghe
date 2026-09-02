package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://openrouter.ai/">OpenRouter</a>.
 *
 * <p>OpenRouter provides access to hundreds of models from various providers
 * through a single API. Use model IDs like {@code "anthropic/claude-sonnet-4"}
 * or {@code "openai/gpt-4o"}.
 *
 * <p>Requires the {@code OPENROUTER_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class OpenRouterProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://openrouter.ai/api/v1";

    public OpenRouterProvider(String apiKey) {
        super("openrouter", apiKey, BASE_URL);
    }

    public OpenRouterProvider(String apiKey, String baseUrl) {
        super("openrouter", apiKey, baseUrl);
    }
}
