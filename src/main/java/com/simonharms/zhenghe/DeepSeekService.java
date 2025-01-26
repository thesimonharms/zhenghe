package com.simonharms.zhenghe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class that provides high-level access to DeepSeek API functionality.
 * Manages chat history and provides methods for interacting with the API.
 */
public class DeepSeekService {
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);

    private final DeepSeekAPIClient client;
    private int defaultMaxTokens; // Default maxTokens value
    private List<DeepSeekModels.ChatMessage> chatHistory; // Chat history for continuous conversations

    /**
     * Constructs a new DeepSeek service with custom configuration.
     *
     * @param apiKey the API key for authentication
     * @param baseUrl the base URL of the DeepSeek API
     * @param defaultMaxTokens the default maximum number of tokens for responses
     */
    public DeepSeekService(String apiKey, String baseUrl, int defaultMaxTokens) {
        this.client = new DeepSeekAPIClient(apiKey, baseUrl);
        this.defaultMaxTokens = defaultMaxTokens;
        this.chatHistory = new ArrayList<>(); // Initialize the chat history
    }

    // Constructor with a default maxTokens value of 50
    public DeepSeekService(String apiKey, String baseUrl) {
        this(apiKey, baseUrl, 50); // Default maxTokens = 50
    }

    // Method to fetch models
    public List<DeepSeekModels.ModelData> getModels() throws DeepSeekAPIException {
        try {
            DeepSeekModels.ModelResponse response = client.sendGetRequest("/models", DeepSeekModels.ModelResponse.class);
            return response.getData(); // Return the list of models
        } catch (IOException e) {
            throw new DeepSeekAPIException("Failed to fetch models", e);
        }
    }

    // Method to generate completions with a custom maxTokens value
    public DeepSeekModels.CompletionResponse generateCompletion(String prompt, int maxTokens) throws DeepSeekAPIException {
        try {
            DeepSeekModels.CompletionRequest request = new DeepSeekModels.CompletionRequest(prompt, maxTokens);
            return client.sendPostRequest("/chat/completions", request, DeepSeekModels.CompletionResponse.class);
        } catch (IOException e) {
            throw new DeepSeekAPIException("Failed to generate completion", e);
        }
    }

    // Method to generate completions using the default maxTokens value
    public DeepSeekModels.CompletionResponse generateCompletion(String prompt) throws DeepSeekAPIException {
        return generateCompletion(prompt, defaultMaxTokens); // Use the default maxTokens
    }

    /**
     * Sends a chat request to the DeepSeek API with message history.
     *
     * @param message the user's message to send
     * @param model the name of the model to use (e.g., "deepseek-chat")
     * @param maxTokens the maximum number of tokens to generate in the response
     * @return the API response containing the model's reply
     * @throws DeepSeekAPIException if the API request fails
     */
    public DeepSeekModels.ChatResponse sendChatRequest(String message, String model, int maxTokens) throws DeepSeekAPIException {
        logger.info("Processing chat request - Model: {} - MaxTokens: {}", model, maxTokens);
        logger.debug("User message: {}", message);

        try {
            // Add the user's message to the chat history
            chatHistory.add(new DeepSeekModels.ChatMessage("user", message));
            logger.debug("Added user message to chat history. History size: {}", chatHistory.size());


            // Changed endpoint to match API docs
            String endpoint = "/chat/completions";
            
            // Create the chat request with the updated chat history
            DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(model, new ArrayList<>(chatHistory), maxTokens);
            logger.debug("Created chat request: {}", request);


            // Send the request and get the response
            DeepSeekModels.ChatResponse response = client.sendPostRequest(endpoint, request, DeepSeekModels.ChatResponse.class);

            // Add the assistant's response to chat history if response messages exist
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty() 
                && response.getChoices().get(0).getMessage() != null) {
                logger.debug("Received {} choices in response", response.getChoices().size());

                chatHistory.add(response.getChoices().get(0).getMessage());

                logger.info("Added assistant response to chat history");
            }

            return response;
        } catch (IOException e) {
            logger.error("Failed to send chat request to model {}", model, e);

            throw new DeepSeekAPIException("Failed to send chat request", e);
        }
    }

    // Method to send a chat request using the default maxTokens value
    public DeepSeekModels.ChatResponse sendChatRequest(String message, String model) throws DeepSeekAPIException {
        return sendChatRequest(message, model, defaultMaxTokens); // Use the default maxTokens
    }

    // Method to clear the chat history
    public void clearChatHistory() {
        chatHistory.clear();
    }

    // Method to update the default maxTokens value
    public void setDefaultMaxTokens(int defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }

    // Method to get the current default maxTokens value
    public int getDefaultMaxTokens() {
        return defaultMaxTokens;
    }
}