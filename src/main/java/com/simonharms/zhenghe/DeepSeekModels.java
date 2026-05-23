package com.simonharms.zhenghe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains all model classes (POJOs) used for JSON serialization/deserialization
 * when communicating with the DeepSeek API.
 */
public class DeepSeekModels {

    private static final Logger logger = LoggerFactory.getLogger(
        DeepSeekModels.class
    );

    // ------------------------------------------------------------------------
    // Active models
    // ------------------------------------------------------------------------

    /**
     * The latest fast-and-cost-effective model. Replaces all previous generation models.
     */
    public static final String DEEPSEEK_V4_FLASH = "deepseek-v4-flash";

    /**
     * The latest high-quality reasoning model.
     */
    public static final String DEEPSEEK_V4_PRO = "deepseek-v4-pro";

    // ------------------------------------------------------------------------
    // Deprecated models — all redirect to flash
    // ------------------------------------------------------------------------

    private static final Set<String> DEPRECATED_MODELS = Set.of(
        "deepseek-chat",
        "deepseek-reasoner",
        "deepseek-coder",
        "deepseek-v2",
        "deepseek-v2-chat",
        "deepseek-v3",
        "deepseek-v3-chat"
    );

    /**
     * Resolves a model identifier to the active model that the API should receive.
     *
     * <ul>
     *   <li><b>Active models</b> ({@link #DEEPSEEK_V4_FLASH}, {@link #DEEPSEEK_V4_PRO}) are returned as-is.</li>
     *   <li><b>Known deprecated models</b> are silently mapped to {@link #DEEPSEEK_V4_FLASH} with a warning log.</li>
     *   <li><b>Unknown models</b> are passed through — the API will handle any errors.</li>
     *   <li><b>Null</b> defaults to {@link #DEEPSEEK_V4_FLASH}.</li>
     * </ul>
     *
     * @param modelId the requested model identifier (may be null)
     * @return the resolved active model identifier
     */
    public static String resolveModel(String modelId) {
        if (modelId == null) {
            return DEEPSEEK_V4_FLASH;
        }

        // Active models — pass through unchanged
        if (
            DEEPSEEK_V4_FLASH.equals(modelId) || DEEPSEEK_V4_PRO.equals(modelId)
        ) {
            return modelId;
        }

        // Known deprecated models — resolve to flash
        if (DEPRECATED_MODELS.contains(modelId)) {
            logger.warn(
                "Model '{}' is deprecated and has been replaced by '{}'. " +
                    "Please update your code to use the new model ID.",
                modelId,
                DEEPSEEK_V4_FLASH
            );
            return DEEPSEEK_V4_FLASH;
        }

        // Unknown model — let the API decide
        logger.debug("Unknown model '{}' — passing through to API", modelId);
        return modelId;
    }

    /**
     * Returns {@code true} if the given model ID is a known deprecated model.
     *
     * @param modelId the model identifier to check
     * @return true if the model is known to be deprecated
     */
    public static boolean isDeprecated(String modelId) {
        return DEPRECATED_MODELS.contains(modelId);
    }

    /**
     * Returns an unmodifiable set of all known deprecated model identifiers.
     *
     * @return the set of deprecated model IDs
     */
    public static Set<String> getDeprecatedModels() {
        return Collections.unmodifiableSet(DEPRECATED_MODELS);
    }

    /**
     * Represents a single message in a chat conversation.
     */
    public static class ChatMessage {

        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;

        @JsonProperty("reasoning_content")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String reasoningContent;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        /**
         * Full constructor including chain-of-thought reasoning content.
         *
         * @param role             the message role
         * @param content          the message text content
         * @param reasoningContent the chain-of-thought reasoning (may be null)
         */
        public ChatMessage(
            String role,
            String content,
            String reasoningContent
        ) {
            this.role = role;
            this.content = content;
            this.reasoningContent = reasoningContent;
        }

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

        public String getReasoningContent() {
            return reasoningContent;
        }

        public void setReasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
        }

        @Override
        public String toString() {
            return (
                "ChatMessage{role='" + role + "', content='" + content + "'}"
            );
        }
    }

    /**
     * Represents the response from the {@code GET /models} endpoint.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelResponse {

        @JsonProperty("object")
        private String object;

        @JsonProperty("data")
        private List<ModelData> data;

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

        @Override
        public String toString() {
            return "ModelResponse{object='" + object + "', data=" + data + "}";
        }
    }

    /**
     * Metadata about a single model available through the API.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelData {

        @JsonProperty("id")
        private String id;

        @JsonProperty("object")
        private String object;

        @JsonProperty("owned_by")
        private String ownedBy;

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

        @Override
        public String toString() {
            return (
                "ModelData{id='" +
                id +
                "', object='" +
                object +
                "', ownedBy='" +
                ownedBy +
                "'}"
            );
        }
    }

    /**
     * Represents a request to the chat completions endpoint.
     * Build the full message list (including any system message) before passing it here —
     * this class is a plain data holder and does not modify the message list.
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

        @JsonProperty("thinking")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private ThinkingConfig thinking;

        @JsonProperty("reasoning_effort")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String reasoningEffort;

        /**
         * Configuration for thinking mode (chain-of-thought reasoning).
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ThinkingConfig {

            @JsonProperty("type")
            private String type = "enabled";

            public ThinkingConfig() {}

            public ThinkingConfig(String type) {
                this.type = type;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }
        }

        /**
         * Represents the response format specification.
         */
        public static class ResponseFormat {

            @JsonProperty("type")
            private String type = "text";

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }
        }

        /**
         * Creates a chat request with the given messages exactly as provided.
         *
         * @param model     the model identifier (e.g., {@code "deepseek-chat"})
         * @param messages  the complete list of messages, including any system message
         * @param maxTokens the maximum number of tokens to generate
         */
        public ChatRequest(
            String model,
            List<ChatMessage> messages,
            int maxTokens
        ) {
            this.model = model;
            this.messages = messages;
            this.maxTokens = maxTokens;
        }

        public List<ChatMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<ChatMessage> messages) {
            this.messages = messages;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getFrequencyPenalty() {
            return frequencyPenalty;
        }

        public void setFrequencyPenalty(double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getPresencePenalty() {
            return presencePenalty;
        }

        public void setPresencePenalty(double presencePenalty) {
            this.presencePenalty = presencePenalty;
        }

        public ResponseFormat getResponseFormat() {
            return responseFormat;
        }

        public void setResponseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
        }

        public Object getStop() {
            return stop;
        }

        public void setStop(Object stop) {
            this.stop = stop;
        }

        public boolean isStream() {
            return stream;
        }

        public void setStream(boolean stream) {
            this.stream = stream;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public double getTopP() {
            return topP;
        }

        public void setTopP(double topP) {
            this.topP = topP;
        }

        public Object getTools() {
            return tools;
        }

        public void setTools(Object tools) {
            this.tools = tools;
        }

        public String getToolChoice() {
            return toolChoice;
        }

        public void setToolChoice(String toolChoice) {
            this.toolChoice = toolChoice;
        }

        public boolean isLogprobs() {
            return logprobs;
        }

        public void setLogprobs(boolean logprobs) {
            this.logprobs = logprobs;
        }

        public Object getTopLogprobs() {
            return topLogprobs;
        }

        public void setTopLogprobs(Object topLogprobs) {
            this.topLogprobs = topLogprobs;
        }

        public ThinkingConfig getThinking() {
            return thinking;
        }

        public void setThinking(ThinkingConfig thinking) {
            this.thinking = thinking;
        }

        public String getReasoningEffort() {
            return reasoningEffort;
        }

        public void setReasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
        }

        @Override
        public String toString() {
            return (
                "ChatRequest{model='" +
                model +
                "', messages=" +
                messages +
                ", maxTokens=" +
                maxTokens +
                ", temperature=" +
                temperature +
                "}"
            );
        }
    }

    /**
     * Represents a response from the chat completions endpoint.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
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
         * A single response choice from the model.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {

            @JsonProperty("finish_reason")
            private String finishReason;

            @JsonProperty("index")
            private int index;

            @JsonProperty("message")
            private ChatMessage message;

            public String getFinishReason() {
                return finishReason;
            }

            public void setFinishReason(String finishReason) {
                this.finishReason = finishReason;
            }

            public int getIndex() {
                return index;
            }

            public void setIndex(int index) {
                this.index = index;
            }

            public ChatMessage getMessage() {
                return message;
            }

            public void setMessage(ChatMessage message) {
                this.message = message;
            }
        }

        /**
         * Token usage statistics for the request and response.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Usage {

            @JsonProperty("completion_tokens")
            private int completionTokens;

            @JsonProperty("prompt_tokens")
            private int promptTokens;

            @JsonProperty("total_tokens")
            private int totalTokens;

            public int getCompletionTokens() {
                return completionTokens;
            }

            public void setCompletionTokens(int completionTokens) {
                this.completionTokens = completionTokens;
            }

            public int getPromptTokens() {
                return promptTokens;
            }

            public void setPromptTokens(int promptTokens) {
                this.promptTokens = promptTokens;
            }

            public int getTotalTokens() {
                return totalTokens;
            }

            public void setTotalTokens(int totalTokens) {
                this.totalTokens = totalTokens;
            }
        }

        /**
         * Extracts the text content of the first response choice.
         *
         * @return the assistant's reply
         * @throws IllegalStateException if the response structure is invalid or content is empty
         */
        public String getMessage() {
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException(
                    "No choices available in API response"
                );
            }
            Choice first = choices.get(0);
            if (first == null) {
                throw new IllegalStateException(
                    "First choice is null in API response"
                );
            }
            ChatMessage msg = first.getMessage();
            if (msg == null) {
                throw new IllegalStateException(
                    "Message is null in API response choice"
                );
            }
            String content = msg.getContent();
            if (content == null || content.isEmpty()) {
                throw new IllegalStateException(
                    "Message content is empty in API response"
                );
            }
            return content;
        }

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

        public long getCreated() {
            return created;
        }

        public void setCreated(long created) {
            this.created = created;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }

        public Usage getUsage() {
            return usage;
        }

        public void setUsage(Usage usage) {
            this.usage = usage;
        }

        @Override
        public String toString() {
            String content = "(no content)";
            if (
                choices != null &&
                !choices.isEmpty() &&
                choices.get(0) != null &&
                choices.get(0).getMessage() != null
            ) {
                content = choices.get(0).getMessage().getContent();
            }
            return (
                "ChatResponse{id='" +
                id +
                "', model='" +
                model +
                "', message='" +
                content +
                "'}"
            );
        }
    }

    /**
     * Represents a single chunk in a streaming chat completion response (SSE).
     * Each chunk carries a content delta for the first choice.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatStreamChunk {

        @JsonProperty("id")
        private String id;

        @JsonProperty("model")
        private String model;

        @JsonProperty("choices")
        private List<StreamChoice> choices;

        /**
         * A single streaming choice containing the delta content.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StreamChoice {

            @JsonProperty("index")
            private int index;

            @JsonProperty("delta")
            private Delta delta;

            @JsonProperty("finish_reason")
            private String finishReason;

            public int getIndex() {
                return index;
            }

            public void setIndex(int index) {
                this.index = index;
            }

            public Delta getDelta() {
                return delta;
            }

            public void setDelta(Delta delta) {
                this.delta = delta;
            }

            public String getFinishReason() {
                return finishReason;
            }

            public void setFinishReason(String finishReason) {
                this.finishReason = finishReason;
            }
        }

        /**
         * The incremental content delta for this chunk.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Delta {

            @JsonProperty("role")
            private String role;

            @JsonProperty("content")
            private String content;

            @JsonProperty("reasoning_content")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private String reasoningContent;

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

            public String getReasoningContent() {
                return reasoningContent;
            }

            public void setReasoningContent(String reasoningContent) {
                this.reasoningContent = reasoningContent;
            }
        }

        /**
         * Returns the content delta from the first choice, or {@code null} if this chunk
         * carries no text content (e.g. the role-announcement chunk or finish chunk).
         *
         * @return the content string, or null
         */
        public String getContent() {
            if (choices == null || choices.isEmpty()) return null;
            Delta delta = choices.get(0).getDelta();
            if (delta == null) return null;
            return delta.getContent();
        }

        /**
         * Returns the reasoning (chain-of-thought) content delta from the first choice,
         * or {@code null} if this chunk carries no reasoning content.
         *
         * @return the reasoning content string, or null
         */
        public String getReasoningContent() {
            if (choices == null || choices.isEmpty()) return null;
            Delta delta = choices.get(0).getDelta();
            if (delta == null) return null;
            return delta.getReasoningContent();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<StreamChoice> getChoices() {
            return choices;
        }

        public void setChoices(List<StreamChoice> choices) {
            this.choices = choices;
        }
    }
}
