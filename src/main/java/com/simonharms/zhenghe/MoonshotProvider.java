package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://platform.kimi.ai/">Moonshot AI / Kimi</a>.
 *
 * <p>Connects to Moonshot's OpenAI-compatible API. Supports Kimi models.
 *
 * <p>Requires the {@code MOONSHOT_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class MoonshotProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.moonshot.cn/v1";

    public MoonshotProvider(String apiKey) {
        super("moonshot", apiKey, BASE_URL);
    }

    public MoonshotProvider(String apiKey, String baseUrl) {
        super("moonshot", apiKey, baseUrl);
    }
}
