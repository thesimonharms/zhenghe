package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://github.com/ggerganov/llama.cpp">llama.cpp</a>.
 *
 * <p>Connects to a local llama.cpp server using its OpenAI-compatible API.
 * No API key required.
 *
 * <p>Default address: {@code http://localhost:8080/v1}
 */
public class LlamaCppProvider extends BaseOpenAICompatProvider {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/v1";

    public LlamaCppProvider() {
        super("llamacpp", DEFAULT_BASE_URL);
    }

    public LlamaCppProvider(String baseUrl) {
        super("llamacpp", baseUrl);
    }
}
