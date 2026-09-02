package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://z.ai/">Zhipu AI / Z.ai (GLM)</a>.
 *
 * <p>Connects to Zhipu's OpenAI-compatible API. Supports GLM models.
 *
 * <p>Requires the {@code ZHIPU_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class ZhipuProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL =
        "https://open.bigmodel.cn/api/paas/v4";

    public ZhipuProvider(String apiKey) {
        super("zhipu", apiKey, BASE_URL);
    }

    public ZhipuProvider(String apiKey, String baseUrl) {
        super("zhipu", apiKey, baseUrl);
    }
}
