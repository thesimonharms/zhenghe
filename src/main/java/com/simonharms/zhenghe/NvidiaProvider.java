package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://build.nvidia.com/">NVIDIA NIM</a>.
 *
 * <p>Connects to NVIDIA's inference API via their OpenAI-compatible endpoint.
 *
 * <p>Requires the {@code NVIDIA_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class NvidiaProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL =
        "https://integrate.api.nvidia.com/v1";

    public NvidiaProvider(String apiKey) {
        super("nvidia", apiKey, BASE_URL);
    }

    public NvidiaProvider(String apiKey, String baseUrl) {
        super("nvidia", apiKey, baseUrl);
    }
}
