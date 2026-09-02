package com.simonharms.zhenghe;

/**
 * Provider for <a href="https://github.com/features/copilot">GitHub Copilot</a>.
 *
 * <p>Connects to GitHub Copilot's chat completions endpoint. Requires a
 * GitHub token with Copilot access.
 *
 * <p>Requires the {@code GITHUB_TOKEN} environment variable or a token
 * passed to the constructor.
 */
public class CopilotProvider extends BaseOpenAICompatProvider {

    private static final String BASE_URL = "https://api.githubcopilot.com";

    public CopilotProvider(String apiKey) {
        super("copilot", apiKey, BASE_URL);
    }

    public CopilotProvider(String apiKey, String baseUrl) {
        super("copilot", apiKey, baseUrl);
    }
}
