package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://platform.deepseek.com/">DeepSeek</a>.
 *
 * <p>Connects to DeepSeek's OpenAI-compatible API. Supports DeepSeek V4 Flash,
 * V4 Pro, and other DeepSeek models.
 *
 * <p>Requires the {@code DEEPSEEK_API_KEY} environment variable or an API key
 * passed to the constructor.
 *
 * <p>This provider uses the generic {@link Provider} interface. For the
 * stateful service with built-in chat history, system prompt, and vision
 * support, use {@link DeepSeekService} instead.
 */
public class DeepSeekProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.deepseek.com";

    public DeepSeekProvider(String apiKey) {
        super("deepseek", apiKey, BASE_URL);
    }

    public DeepSeekProvider(String apiKey, String baseUrl) {
        super("deepseek", apiKey, baseUrl);
    }
}
