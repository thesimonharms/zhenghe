package com.simonharms.zhenghe;

import java.io.Closeable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Core abstraction over any AI provider.
 *
 * <p>Implement this interface to add support for a new provider, or use one
 * of the built-in implementations ({@link OpenAIProvider}, {@link AnthropicProvider},
 * {@link GeminiProvider}, etc.).
 *
 * <p>Provider-agnostic code can accept a {@code Provider} and call
 * {@link #chat(ChatRequest)} or {@link #streamChat(ChatRequest, Consumer)}
 * without knowing which backend is in use.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Provider provider = new GroqProvider(System.getenv("GROQ_API_KEY"));
 * ChatRequest request = new ChatRequest(
 *     "llama-3.3-70b-versatile",
 *     List.of(new ChatMessage("user", "What is the capital of France?")),
 *     512
 * );
 * ChatResponse response = provider.chat(request);
 * System.out.println(response.getMessage());
 * provider.close();
 * }</pre>
 */
public interface Provider extends Closeable {

    /**
     * Send a blocking chat completion request and return the full response.
     *
     * @param request the chat request containing model, messages, and parameters
     * @return the API response
     * @throws ProviderException if the request fails
     */
    DeepSeekModels.ChatResponse chat(DeepSeekModels.ChatRequest request)
        throws ProviderException;

    /**
     * Send a streaming chat request. The {@code onChunk} callback is invoked
     * once per SSE chunk as tokens arrive from the API.
     *
     * @param request the chat request (stream flag is set automatically)
     * @param onChunk called for each content token as it arrives
     * @throws ProviderException if the request fails
     */
    void streamChat(
        DeepSeekModels.ChatRequest request,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws ProviderException;

    /**
     * List all models available from this provider.
     *
     * @return a list of available models
     * @throws ProviderException if the request fails
     */
    List<ModelInfo> listModels() throws ProviderException;

    /**
     * A human-readable identifier for this provider
     * (e.g., {@code "openai"}, {@code "anthropic"}, {@code "groq"}).
     *
     * @return the provider name
     */
    String getName();

    /**
     * Releases the underlying HTTP connection pool and thread pool.
     */
    @Override
    void close();
}
