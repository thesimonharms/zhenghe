package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://deepinfra.com/">DeepInfra</a>.
 *
 * <p>Connects to DeepInfra's OpenAI-compatible API for open-source model inference.
 *
 * <p>Requires the {@code DEEPINFRA_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class DeepInfraProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL =
        "https://api.deepinfra.com/v1/openai";

    public DeepInfraProvider(String apiKey) {
        super("deepinfra", apiKey, BASE_URL);
    }

    public DeepInfraProvider(String apiKey, String baseUrl) {
        super("deepinfra", apiKey, baseUrl);
    }
}
