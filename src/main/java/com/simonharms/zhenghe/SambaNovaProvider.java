package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://cloud.sambanova.ai/">SambaNova</a>.
 *
 * <p>Connects to SambaNova's OpenAI-compatible API.
 *
 * <p>Requires the {@code SAMBANOVA_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class SambaNovaProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.sambanova.ai/v1";

    public SambaNovaProvider(String apiKey) {
        super("sambanova", apiKey, BASE_URL);
    }

    public SambaNovaProvider(String apiKey, String baseUrl) {
        super("sambanova", apiKey, baseUrl);
    }
}
