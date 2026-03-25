package com.simonharms.zhenghe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-level service for interacting with the DeepSeek API.
 *
 * <p>Manages a per-instance chat history so that successive calls to
 * {@link #sendChatRequest} form a continuous conversation. Call
 * {@link #clearChatHistory()} to start a fresh session.
 *
 * <p>Use {@link #generateCompletion(String)} for stateless, single-turn requests
 * that do not affect the conversation history.
 */
public class DeepSeekService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);
    private static final int DEFAULT_MAX_TOKENS = 2048;

    private final DeepSeekAPIClient client;
    private int defaultMaxTokens;
    private final List<DeepSeekModels.ChatMessage> chatHistory;

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
        this.chatHistory = new ArrayList<>();
    }

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

    /**
     * Sends a stateless single-turn completion request. Does not modify chat history.
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
            List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
            messages.add(new DeepSeekModels.ChatMessage("user", prompt));
            DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(model, messages, maxTokens);
            return client.sendPostRequest("/chat/completions", request, DeepSeekModels.ChatResponse.class);
        } catch (IOException e) {
            throw new DeepSeekAPIException("Failed to generate completion", e);
        }
    }

    /**
     * Sends a stateless single-turn completion using the default token limit.
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

    /**
     * Sends a message in the ongoing conversation, preserving full history.
     *
     * <p>The user message is appended to history before the request is sent.
     * The assistant's reply is appended after a successful response.
     *
     * @param message   the user's message
     * @param model     the model to use (e.g., {@code "deepseek-chat"})
     * @param maxTokens the maximum number of tokens to generate
     * @return the API response
     * @throws DeepSeekAPIException if the request fails
     */
    public DeepSeekModels.ChatResponse sendChatRequest(String message, String model, int maxTokens)
            throws DeepSeekAPIException {
        logger.info("Sending chat request — model: {}, maxTokens: {}", model, maxTokens);

        try {
            chatHistory.add(new DeepSeekModels.ChatMessage("user", message));

            DeepSeekModels.ChatRequest request =
                    new DeepSeekModels.ChatRequest(model, new ArrayList<>(chatHistory), maxTokens);

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
     * Returns an unmodifiable view of the current chat history.
     *
     * @return the chat history
     */
    public List<DeepSeekModels.ChatMessage> getChatHistory() {
        return Collections.unmodifiableList(chatHistory);
    }

    /**
     * Clears the chat history, starting a fresh conversation session.
     */
    public void clearChatHistory() {
        chatHistory.clear();
        logger.debug("Chat history cleared");
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
}
