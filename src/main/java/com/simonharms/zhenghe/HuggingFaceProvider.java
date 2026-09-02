package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://huggingface.co/docs/inference-providers">Hugging Face</a>.
 *
 * <p>Connects to Hugging Face's OpenAI-compatible inference router.
 *
 * <p>Requires the {@code HF_TOKEN} environment variable or a token
 * passed to the constructor.
 */
public class HuggingFaceProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL =
        "https://api-inference.huggingface.co/v1";

    public HuggingFaceProvider(String apiKey) {
        super("huggingface", apiKey, BASE_URL);
    }

    public HuggingFaceProvider(String apiKey, String baseUrl) {
        super("huggingface", apiKey, baseUrl);
    }
}
