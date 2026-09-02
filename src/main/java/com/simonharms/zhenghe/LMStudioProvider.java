package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://lmstudio.ai/">LM Studio</a>.
 *
 * <p>Connects to a local LM Studio server using its OpenAI-compatible API.
 * No API key required.
 *
 * <p>Default address: {@code http://localhost:1234/v1}
 */
public class LMStudioProvider extends BaseOpenAICompatProvider {

    private static final String DEFAULT_BASE_URL = "http://localhost:1234/v1";

    public LMStudioProvider() {
        super("lmstudio", DEFAULT_BASE_URL);
    }

    public LMStudioProvider(String baseUrl) {
        super("lmstudio", baseUrl);
    }
}
