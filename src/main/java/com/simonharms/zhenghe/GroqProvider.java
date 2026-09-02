package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://groq.com/">Groq</a>.
 *
 * <p>Groq serves ultra-low-latency inference on its LPU hardware via an
 * OpenAI-compatible API.
 *
 * <p>Requires the {@code GROQ_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class GroqProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.groq.com/openai/v1";

    public GroqProvider(String apiKey) {
        super("groq", apiKey, BASE_URL);
    }

    public GroqProvider(String apiKey, String baseUrl) {
        super("groq", apiKey, baseUrl);
    }
}
