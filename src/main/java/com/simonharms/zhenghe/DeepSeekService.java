package com.simonharms.zhenghe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * High-level service for interacting with the DeepSeek API.
 *
 * <h3>Stateful chat</h3>
 * <p>Successive calls to {@link #sendChatRequest} or {@link #streamChatRequest} share an
 * in-memory conversation history, giving the model context across turns. Call
 * {@link #clearChatHistory()} to start a fresh session.
 *
 * <h3>System prompt</h3>
 * <p>A system message is automatically prepended to every request. The default is
 * {@value DEFAULT_SYSTEM_PROMPT}. Override it with {@link #setSystemPrompt(String)}.
 * Pass {@code null} or an empty string to send no system message.
 *
 * <h3>Thread safety</h3>
 * <p>All methods that read or modify the chat history are {@code synchronized}. A single
 * {@code DeepSeekService} instance is safe to share across threads, but note that
 * concurrent calls to {@link #sendChatRequest} or {@link #streamChatRequest} will be
 * serialized — messages will be sent one at a time in the order the calls arrived.
 * This is usually the desired behaviour for a chat session.
 *
 * <h3>Resource management</h3>
 * <p>Implements {@link Closeable}. Call {@link #close()} when the service is no longer
 * needed to release the underlying HTTP connection pool.
 */
public class DeepSeekService implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);
    private static final int DEFAULT_MAX_TOKENS = 2048;
    static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant";

    private final DeepSeekAPIClient client;
    private int defaultMaxTokens;
    private volatile String systemPrompt = DEFAULT_SYSTEM_PROMPT;
    private final List<DeepSeekModels.ChatMessage> chatHistory = new ArrayList<>();

    /**
     * Constructs a new service with a custom default token limit.
     *
     * @param apiKey           the DeepSeek API key
     * @param baseUrl          the base URL (e.g., {@code "https://api.deepseek.com"})
     * @param defaultMaxTokens the default maximum tokens for each response
     */
    public DeepSeekService(String apiKey, String baseUrl, int defaultMaxTokens) {
        this(new DeepSeekAPIClient(apiKey, baseUrl), defaultMaxTokens);
    }

    /**
     * Constructs a new service using the default token limit of {@value DEFAULT_MAX_TOKENS}.
     *
     * @param apiKey  the DeepSeek API key
     * @param baseUrl the base URL (e.g., {@code "https://api.deepseek.com"})
     */
    public DeepSeekService(String apiKey, String baseUrl) {
        this(apiKey, baseUrl, DEFAULT_MAX_TOKENS);
    }

    /**
     * Package-private constructor for testing — accepts a pre-configured client.
     */
    DeepSeekService(DeepSeekAPIClient client, int defaultMaxTokens) {
        this.client = client;
        this.defaultMaxTokens = defaultMaxTokens;
    }

    // -------------------------------------------------------------------------
    // Models
    // -------------------------------------------------------------------------

    /**
     * Retrieves the list of models available through the API.
     *
     * @return a list of {@link DeepSeekModels.ModelData} objects
     * @throws DeepSeekAPIException if the request fails
     */
    public List<DeepSeekModels.ModelData> getModels() throws DeepSeekAPIException {
        try {
            DeepSeekModels.ModelResponse response =
                    client.sendGetRequest("/models", DeepSeekModels.ModelResponse.class);
            return response.getData();
        } catch (IOException e) {
            throw new DeepSeekAPIException("Failed to fetch models", e);
        }
    }

    // -------------------------------------------------------------------------
    // Single-turn completion (stateless)
    // -------------------------------------------------------------------------

    /**
     * Sends a stateless single-turn request. Does not modify or consult chat history.
     *
     * @param prompt    the prompt to complete
     * @param model     the model to use (e.g., {@code "deepseek-chat"})
     * @param maxTokens the maximum number of tokens to generate
     * @return the API response
     * @throws DeepSeekAPIException if the request fails
     */
    public DeepSeekModels.ChatResponse generateCompletion(String prompt, String model, int maxTokens)
            throws DeepSeekAPIException {
        try {
            List<DeepSeekModels.ChatMessage> messages = buildSystemMessages();
            messages.add(new DeepSeekModels.ChatMessage("user", prompt));
            DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(model, messages, maxTokens);
            return client.sendPostRequest("/chat/completions", request, DeepSeekModels.ChatResponse.class);
        } catch (IOException e) {
            throw new DeepSeekAPIException("Failed to generate completion", e);
        }
    }

    /**
     * Sends a stateless single-turn request using the default token limit.
     *
     * @param prompt the prompt to complete
     * @param model  the model to use
     * @return the API response
     * @throws DeepSeekAPIException if the request fails
     */
    public DeepSeekModels.ChatResponse generateCompletion(String prompt, String model)
            throws DeepSeekAPIException {
        return generateCompletion(prompt, model, defaultMaxTokens);
    }

    // -------------------------------------------------------------------------
    // Stateful chat
    // -------------------------------------------------------------------------

    /**
     * Sends a message in the ongoing conversation, preserving full history.
     *
     * <p>The user message is appended to history before the request is sent.
     * The assistant reply is appended after a successful response. This method
     * is {@code synchronized} — see class-level docs on thread safety.
     *
     * @param message   the user's message
     * @param model     the model to use (e.g., {@code "deepseek-chat"})
     * @param maxTokens the maximum number of tokens to generate
     * @return the API response
     * @throws DeepSeekAPIException if the request fails
     */
    public synchronized DeepSeekModels.ChatResponse sendChatRequest(
            String message, String model, int maxTokens) throws DeepSeekAPIException {

        logger.info("Sending chat request — model: {}, maxTokens: {}", model, maxTokens);
        chatHistory.add(new DeepSeekModels.ChatMessage("user", message));
        List<DeepSeekModels.ChatMessage> messages = buildMessagesSnapshot();

        try {
            DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(model, messages, maxTokens);
            DeepSeekModels.ChatResponse response =
                    client.sendPostRequest("/chat/completions", request, DeepSeekModels.ChatResponse.class);

            if (response != null
                    && response.getChoices() != null
                    && !response.getChoices().isEmpty()
                    && response.getChoices().get(0).getMessage() != null) {
                chatHistory.add(response.getChoices().get(0).getMessage());
                logger.debug("Chat history size: {}", chatHistory.size());
            }
            return response;
        } catch (IOException e) {
            logger.error("Chat request failed for model {}", model, e);
            throw new DeepSeekAPIException("Failed to send chat request", e);
        }
    }

    /**
     * Sends a message using the default token limit.
     *
     * @param message the user's message
     * @param model   the model to use
     * @return the API response
     * @throws DeepSeekAPIException if the request fails
     */
    public DeepSeekModels.ChatResponse sendChatRequest(String message, String model)
            throws DeepSeekAPIException {
        return sendChatRequest(message, model, defaultMaxTokens);
    }

    /**
     * Sends a message in the ongoing conversation and streams the response token by token.
     *
     * <p>The user message is added to history before streaming begins. Once the full
     * response has been received the complete assistant message is appended to history.
     * This method is {@code synchronized} — see class-level docs on thread safety.
     *
     * @param message   the user's message
     * @param model     the model to use (e.g., {@code "deepseek-chat"})
     * @param maxTokens the maximum number of tokens to generate
     * @param onToken   called once for each content token as it arrives; invoked on the
     *                  calling thread while the lock is held — do not call other
     *                  synchronized methods on this service from within the callback
     * @throws DeepSeekAPIException if the request fails
     */
    public synchronized void streamChatRequest(
            String message, String model, int maxTokens, Consumer<String> onToken)
            throws DeepSeekAPIException {

        logger.info("Streaming chat request — model: {}, maxTokens: {}", model, maxTokens);
        chatHistory.add(new DeepSeekModels.ChatMessage("user", message));
        List<DeepSeekModels.ChatMessage> messages = buildMessagesSnapshot();

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(model, messages, maxTokens);
        request.setStream(true);

        StringBuilder fullResponse = new StringBuilder();
        try {
            client.sendStreamingPostRequest("/chat/completions", request, token -> {
                onToken.accept(token);
                fullResponse.append(token);
            });
        } catch (IOException e) {
            logger.error("Streaming chat request failed for model {}", model, e);
            throw new DeepSeekAPIException("Failed to stream chat request", e);
        }

        if (!fullResponse.isEmpty()) {
            chatHistory.add(new DeepSeekModels.ChatMessage("assistant", fullResponse.toString()));
            logger.debug("Chat history size after stream: {}", chatHistory.size());
        }
    }

    /**
     * Streams a response using the default token limit.
     *
     * @param message the user's message
     * @param model   the model to use
     * @param onToken called once per content token as it arrives
     * @throws DeepSeekAPIException if the request fails
     */
    public void streamChatRequest(String message, String model, Consumer<String> onToken)
            throws DeepSeekAPIException {
        streamChatRequest(message, model, defaultMaxTokens, onToken);
    }

    // -------------------------------------------------------------------------
    // History management
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot of the current chat history as an unmodifiable list.
     * Does not include the system message.
     *
     * @return the chat history
     */
    public synchronized List<DeepSeekModels.ChatMessage> getChatHistory() {
        return Collections.unmodifiableList(new ArrayList<>(chatHistory));
    }

    /**
     * Clears the chat history, starting a fresh conversation session.
     * The system prompt is not affected.
     */
    public synchronized void clearChatHistory() {
        chatHistory.clear();
        logger.debug("Chat history cleared");
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Sets the system prompt prepended to every request.
     * Pass {@code null} or an empty string to disable the system message entirely.
     *
     * @param systemPrompt the system prompt text
     */
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    /**
     * Returns the current system prompt, or {@code null} if none is set.
     *
     * @return the system prompt
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Sets the default maximum tokens used when no explicit value is provided.
     *
     * @param defaultMaxTokens the new default
     */
    public void setDefaultMaxTokens(int defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }

    /**
     * Returns the current default maximum tokens.
     *
     * @return the default max tokens
     */
    public int getDefaultMaxTokens() {
        return defaultMaxTokens;
    }

    // -------------------------------------------------------------------------
    // Closeable
    // -------------------------------------------------------------------------

    /**
     * Releases the underlying HTTP connection pool and thread pool.
     */
    @Override
    public void close() {
        client.close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a new list containing just the system message (if set).
     * Used by stateless completion methods.
     */
    private List<DeepSeekModels.ChatMessage> buildSystemMessages() {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new DeepSeekModels.ChatMessage("system", systemPrompt));
        }
        return messages;
    }

    /**
     * Returns a new list of [system message] + current chat history.
     * Must be called while holding the instance lock.
     */
    private List<DeepSeekModels.ChatMessage> buildMessagesSnapshot() {
        List<DeepSeekModels.ChatMessage> messages = buildSystemMessages();
        messages.addAll(chatHistory);
        return messages;
    }
}
