package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://fireworks.ai/">Fireworks AI</a>.
 *
 * <p>Connects to Fireworks AI's OpenAI-compatible API for fast inference.
 *
 * <p>Requires the {@code FIREWORKS_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class FireworksProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL =
        "https://api.fireworks.ai/inference/v1";

    public FireworksProvider(String apiKey) {
        super("fireworks", apiKey, BASE_URL);
    }

    public FireworksProvider(String apiKey, String baseUrl) {
        super("fireworks", apiKey, baseUrl);
    }
}
