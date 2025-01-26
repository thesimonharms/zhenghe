package com.simonharms.zhenghe;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Contains all model classes (POJOs) used for JSON serialization/deserialization
 * in communication with the DeepSeek API.
 */
public class DeepSeekModels {

    /**
     * Represents a single message in a chat conversation.
     */
    public static class ChatMessage {
        @JsonProperty("role")
        private String role; // The role of the message sender (e.g., "user" or "assistant")
        
        @JsonProperty("content")
        private String content; // The content of the message

        // Add no-args constructor for Jackson
        public ChatMessage() {
        }

        // Constructor to initialize the fields
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // Getters and setters
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        // Override toString() for easy debugging
        @Override
        public String toString() {
            return "ChatMessage{role='" + role + "', content='" + content + "'}";
        }
    }

    /**
     * Represents the response from the GET /models endpoint.
     * Contains a list of available models and their metadata.
     */
    public static class ModelResponse {
        @JsonProperty("object")
        private String object; // The type of object (e.g., "list")

        @JsonProperty("data")
        private List<ModelData> data; // The list of models

        // Getters and setters
        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public List<ModelData> getData() {
            return data;
        }

        public void setData(List<ModelData> data) {
            this.data = data;
        }

        // Override toString() for easy debugging
        @Override
        public String toString() {
            return "ModelResponse{object='" + object + "', data=" + data + "}";
        }
    }

    /**
     * Represents metadata about a single model available through the API.
     */
    public static class ModelData {
        @JsonProperty("id")
        private String id; // The model ID (e.g., "deepseek-chat")

        @JsonProperty("object")
        private String object; // The type of object (e.g., "model")

        @JsonProperty("owned_by")
        private String ownedBy; // The owner of the model (e.g., "deepseek")

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public String getOwnedBy() {
            return ownedBy;
        }

        public void setOwnedBy(String ownedBy) {
            this.ownedBy = ownedBy;
        }

        // Override toString() for easy debugging
        @Override
        public String toString() {
            return "ModelData{id='" + id + "', object='" + object + "', ownedBy='" + ownedBy + "'}";
        }
    }

    /**
     * Represents a request to the completions endpoint.
     * Used for generating text completions from a prompt.
     */
    public static class CompletionRequest {
        private String prompt; // The input prompt for the API
        private int maxTokens; // The maximum number of tokens to generate

        // Constructor to initialize the fields
        public CompletionRequest(String prompt, int maxTokens) {
            this.prompt = prompt;
            this.maxTokens = maxTokens;
        }

        // Getters and setters
        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        // Override toString() for easy debugging
        @Override
        public String toString() {
            return "CompletionRequest{prompt='" + prompt + "', maxTokens=" + maxTokens + "}";
        }
    }

    /**
     * Represents a response from the completions endpoint.
     * Contains the generated text completion.
     */
    public static class CompletionResponse {
        private String id; // Unique ID for the completion
        private String text; // The generated text

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        // Override toString() for easy debugging
        @Override
        public String toString() {
            return "CompletionResponse{id='" + id + "', text='" + text + "'}";
        }
    }

    /**
     * Represents a request to the chat completion endpoint.
     * Contains messages and parameters that control the response generation.
     */
    public static class ChatRequest {
        @JsonProperty("messages")
        private List<ChatMessage> messages;

        @JsonProperty("model")
        private String model;

        @JsonProperty("frequency_penalty")
        private double frequencyPenalty = 0;

        @JsonProperty("max_tokens")
        private int maxTokens = 2048;

        @JsonProperty("presence_penalty")
        private double presencePenalty = 0;

        @JsonProperty("response_format")
        private ResponseFormat responseFormat = new ResponseFormat();

        @JsonProperty("stop")
        private Object stop = null;

        @JsonProperty("stream")
        private boolean stream = false;

        @JsonProperty("stream_options")
        private Object streamOptions = null;

        @JsonProperty("temperature")
        private double temperature = 1.0;

        @JsonProperty("top_p")
        private double topP = 1.0;

        @JsonProperty("tools")
        private Object tools = null;

        @JsonProperty("tool_choice")
        private String toolChoice = "none";

        @JsonProperty("logprobs")
        private boolean logprobs = false;

        @JsonProperty("top_logprobs")
        private Object topLogprobs = null;

        /**
         * Represents the format specification for the response.
         */
        public static class ResponseFormat {
            @JsonProperty("type")
            private String type = "text";

            // Getters and setters
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
        }

        /**
         * Creates a new chat request with the specified parameters.
         *
         * @param model the model to use for generation
         * @param messages the list of chat messages
         * @param maxTokens the maximum number of tokens to generate
         */
        public ChatRequest(String model, List<ChatMessage> messages, int maxTokens) {
            this.model = model;
            this.messages = messages;
            this.maxTokens = maxTokens;
            
            // Add system message if not present
            if (messages.isEmpty() || !messages.get(0).getRole().equals("system")) {
                List<ChatMessage> newMessages = new ArrayList<>();
                newMessages.add(new ChatMessage("system", "You are a helpful assistant"));
                newMessages.addAll(messages);
                this.messages = newMessages;
            }
        }

        // Getters and setters
        public List<ChatMessage> getMessages() { return messages; }
        public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public double getFrequencyPenalty() { return frequencyPenalty; }
        public void setFrequencyPenalty(double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getPresencePenalty() { return presencePenalty; }
        public void setPresencePenalty(double presencePenalty) { this.presencePenalty = presencePenalty; }
        public ResponseFormat getResponseFormat() { return responseFormat; }
        public void setResponseFormat(ResponseFormat responseFormat) { this.responseFormat = responseFormat; }
        public Object getStop() { return stop; }
        public void setStop(Object stop) { this.stop = stop; }
        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
        public Object getStreamOptions() { return streamOptions; }
        public void setStreamOptions(Object streamOptions) { this.streamOptions = streamOptions; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public double getTopP() { return topP; }
        public void setTopP(double topP) { this.topP = topP; }
        public Object getTools() { return tools; }
        public void setTools(Object tools) { this.tools = tools; }
        public String getToolChoice() { return toolChoice; }
        public void setToolChoice(String toolChoice) { this.toolChoice = toolChoice; }
        public boolean isLogprobs() { return logprobs; }
        public void setLogprobs(boolean logprobs) { this.logprobs = logprobs; }
        public Object getTopLogprobs() { return topLogprobs; }
        public void setTopLogprobs(Object topLogprobs) { this.topLogprobs = topLogprobs; }

        @Override
        public String toString() {
            return "ChatRequest{messages=" + messages + 
                   ", maxTokens=" + maxTokens + 
                   ", temperature=" + temperature + 
                   ", topP=" + topP + "}";
        }
    }

    /**
     * Represents a response from the chat completion endpoint.
     * Contains the generated message and metadata about the response.
     */
    public static class ChatResponse {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("object")
        private String object;
        
        @JsonProperty("created")
        private long created;
        
        @JsonProperty("model")
        private String model;
        
        @JsonProperty("choices")
        private List<Choice> choices;
        
        @JsonProperty("usage")
        private Usage usage;

        /**
         * Represents a single response choice from the model.
         * Contains the generated message and metadata about its generation.
         */
        public static class Choice {
            @JsonProperty("finish_reason")
            private String finishReason;
            
            @JsonProperty("index")
            private int index;
            
            @JsonProperty("message")
            private ChatMessage message;

            // Getters and setters
            public String getFinishReason() { return finishReason; }
            public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
            public int getIndex() { return index; }
            public void setIndex(int index) { this.index = index; }
            public ChatMessage getMessage() { return message; }
            public void setMessage(ChatMessage message) { this.message = message; }
        }

        /**
         * Contains token usage statistics for the request and response.
         */
        public static class Usage {
            @JsonProperty("completion_tokens")
            private int completionTokens;
            
            @JsonProperty("prompt_tokens")
            private int promptTokens;
            
            @JsonProperty("total_tokens")
            private int totalTokens;

            // Getters and setters
            public int getCompletionTokens() { return completionTokens; }
            public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
            public int getPromptTokens() { return promptTokens; }
            public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
            public int getTotalTokens() { return totalTokens; }
            public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
        }

        /**
         * Extracts the message content from the API response.
         *
         * @return the content of the message from the model
         * @throws IllegalStateException if the response structure is invalid
         */
        public String getMessage() throws IllegalStateException {
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("No choices available in API response");
            }

            Choice firstChoice = choices.get(0);
            if (firstChoice == null) {
                throw new IllegalStateException("First choice is null in API response");
            }

            ChatMessage message = firstChoice.getMessage();
            if (message == null) {
                throw new IllegalStateException("Message object is null in API response choice");
            }

            String content = message.getContent();
            if (content == null || content.isEmpty()) {
                throw new IllegalStateException("Message content is empty in API response");
            }

            return content;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getObject() { return object; }
        public void setObject(String object) { this.object = object; }
        public long getCreated() { return created; }
        public void setCreated(long created) { this.created = created; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<Choice> getChoices() { return choices; }
        public void setChoices(List<Choice> choices) { this.choices = choices; }
        public Usage getUsage() { return usage; }
        public void setUsage(Usage usage) { this.usage = usage; }

        @Override
        public String toString() {
            return "ChatResponse{id='" + id + "', object='" + object + "', message='" + getMessage() + "'}";
        }
    }
}