package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://docs.x.ai/">xAI Grok</a>.
 *
 * <p>Connects to xAI's OpenAI-compatible API. Supports Grok models.
 *
 * <p>Requires the {@code XAI_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class XAIProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.x.ai/v1";

    public XAIProvider(String apiKey) {
        super("xai", apiKey, BASE_URL);
    }

    public XAIProvider(String apiKey, String baseUrl) {
        super("xai", apiKey, baseUrl);
    }
}
