package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://dashscope.aliyun.com/">Alibaba Qwen</a> (DashScope).
 *
 * <p>Connects to Alibaba's DashScope API in OpenAI-compatible mode.
 * Supports Qwen models.
 *
 * <p>Requires the {@code DASHSCOPE_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class QwenProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL =
        "https://dashscope.aliyuncs.com/compatible-mode/v1";

    public QwenProvider(String apiKey) {
        super("qwen", apiKey, BASE_URL);
    }

    public QwenProvider(String apiKey, String baseUrl) {
        super("qwen", apiKey, baseUrl);
    }
}
