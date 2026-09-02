package com.simonharms.zhenghe;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of all supported AI providers.
 *
 * <p>Provides convenient factory methods to create provider instances
 * by enum constant, with optional environment variable resolution.
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * // Create from API key directly
 * Provider provider = ProviderType.GROQ.create(System.getenv("GROQ_API_KEY"));
 *
 * // Create using default env var
 * Provider provider = ProviderType.OPENAI.create();
 *
 * // Create with custom base URL (e.g., for proxies)
 * Provider provider = ProviderType.DEEPSEEK.create(apiKey, "https://my-proxy.com/v1");
 *
 * // Local providers need no key
 * Provider provider = ProviderType.OLLAMA.create();
 * }</pre>
 */
public enum ProviderType {
    OPENAI("openai", "https://api.openai.com/v1", "OPENAI_API_KEY"),
    ANTHROPIC(
        "anthropic",
        "https://api.anthropic.com/v1",
        "ANTHROPIC_API_KEY"
    ),
    GEMINI("gemini", "https://generativelanguage.googleapis.com/v1beta", "GEMINI_API_KEY"),
    XAI("xai", "https://api.x.ai/v1", "XAI_API_KEY"),
    MISTRAL("mistral", "https://api.mistral.ai/v1", "MISTRAL_API_KEY"),
    DEEPSEEK("deepseek", "https://api.deepseek.com", "DEEPSEEK_API_KEY"),
    COPILOT("copilot", "https://api.githubcopilot.com", "GITHUB_TOKEN"),
    OPENROUTER(
        "openrouter",
        "https://openrouter.ai/api/v1",
        "OPENROUTER_API_KEY"
    ),
    MOONSHOT("moonshot", "https://api.moonshot.cn/v1", "MOONSHOT_API_KEY"),
    PERPLEXITY("perplexity", "https://api.perplexity.ai", "PERPLEXITY_API_KEY"),
    NOUS("nous", "https://api.nousresearch.com/v1", "NOUS_API_KEY"),
    QWEN(
        "qwen",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "DASHSCOPE_API_KEY"
    ),
    GROQ("groq", "https://api.groq.com/openai/v1", "GROQ_API_KEY"),
    TOGETHER("together", "https://api.together.xyz/v1", "TOGETHER_API_KEY"),
    FIREWORKS(
        "fireworks",
        "https://api.fireworks.ai/inference/v1",
        "FIREWORKS_API_KEY"
    ),
    CEREBRAS("cerebras", "https://api.cerebras.ai/v1", "CEREBRAS_API_KEY"),
    SAMBANOVA(
        "sambanova",
        "https://api.sambanova.ai/v1",
        "SAMBANOVA_API_KEY"
    ),
    ZHIPU(
        "zhipu",
        "https://open.bigmodel.cn/api/paas/v4",
        "ZHIPU_API_KEY"
    ),
    MINIMAX("minimax", "https://api.minimax.chat/v1", "MINIMAX_API_KEY"),
    HUGGINGFACE(
        "huggingface",
        "https://api-inference.huggingface.co/v1",
        "HF_TOKEN"
    ),
    NVIDIA(
        "nvidia",
        "https://integrate.api.nvidia.com/v1",
        "NVIDIA_API_KEY"
    ),
    DEEPINFRA(
        "deepinfra",
        "https://api.deepinfra.com/v1/openai",
        "DEEPINFRA_API_KEY"
    ),
    CLOUDFLARE(
        "cloudflare",
        "https://api.cloudflare.com/client/v4",
        null
    ),
    OLLAMA("ollama", "http://localhost:11434", null),
    LMSTUDIO("lmstudio", "http://localhost:1234/v1", null),
    LLAMACPP("llamacpp", "http://localhost:8080/v1", null);

    private final String name;
    private final String defaultBaseUrl;
    private final String envVar;

    // Quick lookup by name
    private static final Map<String, ProviderType> BY_NAME = new HashMap<>();

    static {
        for (ProviderType type : values()) {
            BY_NAME.put(type.name, type);
        }
    }

    ProviderType(String name, String defaultBaseUrl, String envVar) {
        this.name = name;
        this.defaultBaseUrl = defaultBaseUrl;
        this.envVar = envVar;
    }

    /**
     * Returns the provider name identifier (e.g., {@code "openai"}, {@code "groq"}).
     *
     * @return the provider name
     */
    public String getProviderName() {
        return name;
    }

    /**
     * Returns the default base URL for this provider.
     *
     * @return the base URL
     */
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * Returns the environment variable name for the API key, or {@code null}
     * for providers that don't require authentication (Ollama, LM Studio, llama.cpp).
     *
     * @return the env var name, or null
     */
    public String getEnvVar() {
        return envVar;
    }

    /**
     * Look up a provider type by its name string.
     *
     * @param name the provider name (case-insensitive)
     * @return the ProviderType, or null if not found
     */
    public static ProviderType fromName(String name) {
        return BY_NAME.get(name.toLowerCase());
    }

    /**
     * Creates a provider using the given API key.
     *
     * @param apiKey the API key (may be null for local providers)
     * @return a new Provider instance
     * @throws IllegalArgumentException if a key is required but null
     */
    public Provider create(String apiKey) {
        if (apiKey == null && envVar != null) {
            throw new IllegalArgumentException(
                name +
                    " requires an API key. Pass one to create() or set the " +
                    envVar +
                    " environment variable."
            );
        }
        return switch (this) {
            case OPENAI -> new OpenAIProvider(apiKey);
            case ANTHROPIC -> new AnthropicProvider(apiKey);
            case GEMINI -> new GeminiProvider(apiKey);
            case XAI -> new XAIProvider(apiKey);
            case MISTRAL -> new MistralProvider(apiKey);
            case DEEPSEEK -> new DeepSeekProvider(apiKey);
            case COPILOT -> new CopilotProvider(apiKey);
            case OPENROUTER -> new OpenRouterProvider(apiKey);
            case MOONSHOT -> new MoonshotProvider(apiKey);
            case PERPLEXITY -> new PerplexityProvider(apiKey);
            case NOUS -> new NousProvider(apiKey);
            case QWEN -> new QwenProvider(apiKey);
            case GROQ -> new GroqProvider(apiKey);
            case TOGETHER -> new TogetherProvider(apiKey);
            case FIREWORKS -> new FireworksProvider(apiKey);
            case CEREBRAS -> new CerebrasProvider(apiKey);
            case SAMBANOVA -> new SambaNovaProvider(apiKey);
            case ZHIPU -> new ZhipuProvider(apiKey);
            case MINIMAX -> new MiniMaxProvider(apiKey);
            case HUGGINGFACE -> new HuggingFaceProvider(apiKey);
            case NVIDIA -> new NvidiaProvider(apiKey);
            case DEEPINFRA -> new DeepInfraProvider(apiKey);
            case CLOUDFLARE ->
                throw new IllegalArgumentException(
                    "Cloudflare requires both account ID and API token. Use new CloudflareProvider(accountId, token) directly."
                );
            case OLLAMA -> new OllamaProvider();
            case LMSTUDIO -> new LMStudioProvider();
            case LLAMACPP -> new LlamaCppProvider();
        };
    }

    /**
     * Creates a provider using the API key from the default environment variable.
     * For local providers (Ollama, LM Studio, llama.cpp), no key is needed.
     *
     * @return a new Provider instance
     * @throws IllegalArgumentException if the env var is not set for providers
     *                                  that require a key
     */
    public Provider create() {
        if (envVar == null) {
            return create(null);
        }
        String key = System.getenv(envVar);
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(
                name +
                    " requires the " +
                    envVar +
                    " environment variable to be set."
            );
        }
        return create(key);
    }

    /**
     * Creates a provider with a custom base URL (useful for proxies or testing).
     *
     * @param apiKey  the API key
     * @param baseUrl the custom base URL
     * @return a new Provider instance
     */
    public Provider create(String apiKey, String baseUrl) {
        return switch (this) {
            case OPENAI -> new OpenAIProvider(apiKey, baseUrl);
            case ANTHROPIC -> new AnthropicProvider(apiKey, baseUrl);
            case GEMINI -> new GeminiProvider(apiKey, baseUrl);
            case XAI -> new XAIProvider(apiKey, baseUrl);
            case MISTRAL -> new MistralProvider(apiKey, baseUrl);
            case DEEPSEEK -> new DeepSeekProvider(apiKey, baseUrl);
            case COPILOT -> new CopilotProvider(apiKey, baseUrl);
            case OPENROUTER -> new OpenRouterProvider(apiKey, baseUrl);
            case MOONSHOT -> new MoonshotProvider(apiKey, baseUrl);
            case PERPLEXITY -> new PerplexityProvider(apiKey, baseUrl);
            case NOUS -> new NousProvider(apiKey, baseUrl);
            case QWEN -> new QwenProvider(apiKey, baseUrl);
            case GROQ -> new GroqProvider(apiKey, baseUrl);
            case TOGETHER -> new TogetherProvider(apiKey, baseUrl);
            case FIREWORKS -> new FireworksProvider(apiKey, baseUrl);
            case CEREBRAS -> new CerebrasProvider(apiKey, baseUrl);
            case SAMBANOVA -> new SambaNovaProvider(apiKey, baseUrl);
            case ZHIPU -> new ZhipuProvider(apiKey, baseUrl);
            case MINIMAX -> new MiniMaxProvider(apiKey, baseUrl);
            case HUGGINGFACE -> new HuggingFaceProvider(apiKey, baseUrl);
            case NVIDIA -> new NvidiaProvider(apiKey, baseUrl);
            case DEEPINFRA -> new DeepInfraProvider(apiKey, baseUrl);
            case CLOUDFLARE ->
                throw new IllegalArgumentException(
                    "Cloudflare requires both account ID and API token. Use new CloudflareProvider(accountId, token, baseUrl) directly."
                );
            case OLLAMA -> new OllamaProvider(baseUrl);
            case LMSTUDIO -> new LMStudioProvider(baseUrl);
            case LLAMACPP -> new LlamaCppProvider(baseUrl);
        };
    }
}
