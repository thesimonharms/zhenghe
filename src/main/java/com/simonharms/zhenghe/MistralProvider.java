package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://mistral.ai/">Mistral</a>.
 *
 * <p>Connects to Mistral's OpenAI-compatible API. Supports Mistral, Mixtral,
 * Codestral, and other Mistral models.
 *
 * <p>Requires the {@code MISTRAL_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class MistralProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.mistral.ai/v1";

    public MistralProvider(String apiKey) {
        super("mistral", apiKey, BASE_URL);
    }

    public MistralProvider(String apiKey, String baseUrl) {
        super("mistral", apiKey, baseUrl);
    }
}
