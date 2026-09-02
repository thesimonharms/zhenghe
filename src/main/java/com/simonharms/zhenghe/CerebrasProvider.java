package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://inference.cerebras.ai/">Cerebras</a>.
 *
 * <p>Connects to Cerebras' OpenAI-compatible API for fast inference on their
 * wafer-scale hardware.
 *
 * <p>Requires the {@code CEREBRAS_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class CerebrasProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.cerebras.ai/v1";

    public CerebrasProvider(String apiKey) {
        super("cerebras", apiKey, BASE_URL);
    }

    public CerebrasProvider(String apiKey, String baseUrl) {
        super("cerebras", apiKey, baseUrl);
    }
}
