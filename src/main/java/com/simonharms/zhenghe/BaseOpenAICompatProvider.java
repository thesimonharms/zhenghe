package com.simonharms.zhenghe;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base for OpenAI-compatible providers.
 *
 * <p>Handles serialization, deserialization, and error wrapping. Subclasses
 * only need to supply the provider name, API key, and base URL.
 *
 * <p>Most AI providers (OpenAI, Groq, Mistral, DeepSeek, Together, etc.)
 * speak the same wire format. This class eliminates the boilerplate that
 * would otherwise be duplicated across 20+ provider classes.
 */
public abstract class BaseOpenAICompatProvider implements Provider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String name;
    protected final OpenAICompatClient client;

    /**
     * Creates a provider with API key authentication.
     *
     * @param name    the provider name identifier
     * @param apiKey  the API key
     * @param baseUrl the base URL
     */
    protected BaseOpenAICompatProvider(
        String name,
        String apiKey,
        String baseUrl
    ) {
        this.name = name;
        this.client = new OpenAICompatClient(apiKey, baseUrl);
    }

    /**
     * Creates a provider without API key authentication (for local providers).
     *
     * @param name    the provider name identifier
     * @param baseUrl the base URL
     */
    protected BaseOpenAICompatProvider(String name, String baseUrl) {
        this.name = name;
        this.client = new OpenAICompatClient(baseUrl);
    }

    /**
     * Package-private constructor for testing with a pre-configured client.
     */
    BaseOpenAICompatProvider(String name, OpenAICompatClient client) {
        this.name = name;
        this.client = client;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DeepSeekModels.ChatResponse chat(
        DeepSeekModels.ChatRequest request
    ) throws ProviderException {
        try {
            logger.debug(
                "Sending chat request to {} — model: {}",
                name,
                request.getModel()
            );
            String json = client
                .getObjectMapper()
                .writeValueAsString(request);
            String responseJson = client.chat("/chat/completions", json);
            DeepSeekModels.ChatResponse response = client
                .getObjectMapper()
                .readValue(responseJson, DeepSeekModels.ChatResponse.class);
            logger.debug(
                "Received response from {} — id: {}",
                name,
                response.getId()
            );
            return response;
        } catch (OpenAICompatClient.ProviderHTTPException e) {
            throw new ProviderException(
                name + " API error: " + e.getMessage(),
                e,
                e.getStatusCode(),
                name
            );
        } catch (IOException e) {
            throw new ProviderException(
                name + " request failed: " + e.getMessage(),
                e,
                -1,
                name
            );
        }
    }

    @Override
    public void streamChat(
        DeepSeekModels.ChatRequest request,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws ProviderException {
        try {
            logger.debug(
                "Starting streaming chat to {} — model: {}",
                name,
                request.getModel()
            );
            request.setStream(true);
            String json = client
                .getObjectMapper()
                .writeValueAsString(request);
            client.streamChat("/chat/completions", json, onChunk);
        } catch (OpenAICompatClient.ProviderHTTPException e) {
            throw new ProviderException(
                name + " streaming error: " + e.getMessage(),
                e,
                e.getStatusCode(),
                name
            );
        } catch (IOException e) {
            throw new ProviderException(
                name + " streaming request failed: " + e.getMessage(),
                e,
                -1,
                name
            );
        }
    }

    @Override
    public List<ModelInfo> listModels() throws ProviderException {
        try {
            logger.debug("Listing models from {}", name);
            String json = client.getModels("/models");
            DeepSeekModels.ModelResponse response = client
                .getObjectMapper()
                .readValue(json, DeepSeekModels.ModelResponse.class);
            return response
                .getData()
                .stream()
                .map(m -> new ModelInfo(m.getId(), m.getOwnedBy()))
                .collect(Collectors.toList());
        } catch (OpenAICompatClient.ProviderHTTPException e) {
            throw new ProviderException(
                name + " models error: " + e.getMessage(),
                e,
                e.getStatusCode(),
                name
            );
        } catch (IOException e) {
            throw new ProviderException(
                name + " models request failed: " + e.getMessage(),
                e,
                -1,
                name
            );
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
