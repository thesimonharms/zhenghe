package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://www.together.ai/">Together AI</a>.
 *
 * <p>Connects to Together AI's OpenAI-compatible API for open-source model inference.
 *
 * <p>Requires the {@code TOGETHER_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class TogetherProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.together.xyz/v1";

    public TogetherProvider(String apiKey) {
        super("together", apiKey, BASE_URL);
    }

    public TogetherProvider(String apiKey, String baseUrl) {
        super("together", apiKey, baseUrl);
    }
}
