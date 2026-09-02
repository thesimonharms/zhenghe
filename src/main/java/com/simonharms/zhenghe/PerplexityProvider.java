package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://docs.perplexity.ai/">Perplexity</a>.
 *
 * <p>Connects to Perplexity's API. Supports Sonar models for
 * search-augmented generation.
 *
 * <p>Requires the {@code PERPLEXITY_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class PerplexityProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.perplexity.ai";

    public PerplexityProvider(String apiKey) {
        super("perplexity", apiKey, BASE_URL);
    }

    public PerplexityProvider(String apiKey, String baseUrl) {
        super("perplexity", apiKey, baseUrl);
    }
}
