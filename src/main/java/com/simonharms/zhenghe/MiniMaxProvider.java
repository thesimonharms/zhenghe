package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://www.minimax.io/">MiniMax</a>.
 *
 * <p>Connects to MiniMax's OpenAI-compatible API.
 *
 * <p>Requires the {@code MINIMAX_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class MiniMaxProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.minimax.chat/v1";

    public MiniMaxProvider(String apiKey) {
        super("minimax", apiKey, BASE_URL);
    }

    public MiniMaxProvider(String apiKey, String baseUrl) {
        super("minimax", apiKey, baseUrl);
    }
}
