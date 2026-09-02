package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://portal.nousresearch.com/">Nous Portal</a>.
 *
 * <p>Connects to Nous Research's API. Supports Hermes models.
 *
 * <p>Requires the {@code NOUS_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class NousProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.nousresearch.com/v1";

    public NousProvider(String apiKey) {
        super("nous", apiKey, BASE_URL);
    }

    public NousProvider(String apiKey, String baseUrl) {
        super("nous", apiKey, baseUrl);
    }
}
