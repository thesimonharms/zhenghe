package com.simonharms.zhenghe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for the <a href="https://www.anthropic.com/">Anthropic</a> API (Claude models).
 *
 * <p>The Anthropic API uses a different request/response format from
 * OpenAI-compatible providers. This class handles the conversion automatically —
 * system prompts, message roles, content parts, and usage statistics are all
 * mapped to the common types.
 *
 * <p>{@code max_tokens} is required by Anthropic; if not set on the request,
 * a default of 4096 is used.
 *
 * <p>Requires the {@code ANTHROPIC_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class AnthropicProvider implements Provider {

    private static final Logger logger = LoggerFactory.getLogger(
        AnthropicProvider.class
    );

    private static final String BASE_URL = "https://api.anthropic.com/v1";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 4096;

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicProvider(String apiKey) {
        this(apiKey, BASE_URL);
    }

    public AnthropicProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        this.objectMapper = OpenAICompatClient.defaultObjectMapper();
    }

    AnthropicProvider(
        String apiKey,
        String baseUrl,
        OkHttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "anthropic";
    }

    @Override
    public DeepSeekModels.ChatResponse chat(
        DeepSeekModels.ChatRequest request
    ) throws ProviderException {
        try {
            AnthropicRequest body = toAnthropicRequest(request, false);
            String json = objectMapper.writeValueAsString(body);

            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(
                    RequestBody.create(
                        json,
                        MediaType.parse("application/json")
                    )
                )
                .build();

            try (
                Response response = httpClient.newCall(httpRequest).execute()
            ) {
                String responseBody =
                    response.body() != null ? response.body().string() : null;

                if (!response.isSuccessful() || responseBody == null) {
                    throw new ProviderException(
                        "Anthropic API error: HTTP " + response.code(),
                        null,
                        response.code(),
                        "anthropic"
                    );
                }

                AnthropicResponse anthropicResponse = objectMapper.readValue(
                    responseBody,
                    AnthropicResponse.class
                );
                return fromAnthropicResponse(
                    anthropicResponse,
                    request.getModel()
                );
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Anthropic request failed: " + e.getMessage(),
                e,
                -1,
                "anthropic"
            );
        }
    }

    @Override
    public void streamChat(
        DeepSeekModels.ChatRequest request,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws ProviderException {
        try {
            AnthropicRequest body = toAnthropicRequest(request, true);
            String json = objectMapper.writeValueAsString(body);

            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .post(
                    RequestBody.create(
                        json,
                        MediaType.parse("application/json")
                    )
                )
                .build();

            try (
                Response response = httpClient.newCall(httpRequest).execute()
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    String errorBody =
                        response.body() != null
                            ? response.body().string()
                            : "(empty)";
                    throw new ProviderException(
                        "Anthropic streaming error: HTTP " +
                            response.code() +
                            " — " +
                        errorBody,
                        null,
                        response.code(),
                        "anthropic"
                    );
                }

                parseAnthropicSSE(response.body().byteStream(), onChunk);
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Anthropic streaming request failed: " + e.getMessage(),
                e,
                -1,
                "anthropic"
            );
        }
    }

    @Override
    public List<ModelInfo> listModels() throws ProviderException {
        try {
            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/models")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Accept", "application/json")
                .get()
                .build();

            try (
                Response response = httpClient.newCall(httpRequest).execute()
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new ProviderException(
                        "Anthropic models error: HTTP " + response.code(),
                        null,
                        response.code(),
                        "anthropic"
                    );
                }

                String responseBody = response.body().string();
                AnthropicModelList list = objectMapper.readValue(
                    responseBody,
                    AnthropicModelList.class
                );
                List<ModelInfo> result = new ArrayList<>();
                if (list.data != null) {
                    for (AnthropicModelEntry entry : list.data) {
                        result.add(
                            new ModelInfo(entry.id, "anthropic")
                        );
                    }
                }
                return result;
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Anthropic models request failed: " + e.getMessage(),
                e,
                -1,
                "anthropic"
            );
        }
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    // -------------------------------------------------------------------------
    // Request/Response conversion
    // -------------------------------------------------------------------------

    private AnthropicRequest toAnthropicRequest(
        DeepSeekModels.ChatRequest request,
        boolean stream
    ) {
        // Extract system messages into the top-level system field
        StringBuilder systemBuilder = new StringBuilder();
        for (DeepSeekModels.ChatMessage msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) {
                if (systemBuilder.length() > 0) systemBuilder.append("\n");
                systemBuilder.append(msg.getContent());
            }
        }

        // Build messages (excluding system)
        List<AnthropicMessage> messages = new ArrayList<>();
        for (DeepSeekModels.ChatMessage msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) continue;
            messages.add(toAnthropicMessage(msg));
        }

        AnthropicRequest body = new AnthropicRequest();
        body.model = request.getModel();
        body.max_tokens =
            request.getMaxTokens() > 0
                ? request.getMaxTokens()
                : DEFAULT_MAX_TOKENS;
        body.messages = messages;
        body.system =
            systemBuilder.length() > 0 ? systemBuilder.toString() : null;
        body.stream = stream;
        body.temperature =
            request.getTemperature() != 1.0
                ? request.getTemperature()
                : null;
        return body;
    }

    private AnthropicMessage toAnthropicMessage(DeepSeekModels.ChatMessage msg) {
        AnthropicMessage m = new AnthropicMessage();
        m.role = "user".equals(msg.getRole()) ? "user" : "assistant";

        // Multimodal content
        List<DeepSeekModels.ContentPart> parts = msg.getContentParts();
        if (parts != null) {
            List<AnthropicContentBlock> blocks = new ArrayList<>();
            for (DeepSeekModels.ContentPart part : parts) {
                switch (part.getType()) {
                    case "text":
                        blocks.add(
                            new AnthropicContentBlock("text", part.getText())
                        );
                        break;
                    case "image_url":
                        DeepSeekModels.ContentPart.ImageUrl imgUrl =
                            part.getImageUrl();
                        if (imgUrl != null && imgUrl.getUrl() != null) {
                            AnthropicImageSource source =
                                new AnthropicImageSource();
                            source.type = "url";
                            source.url = imgUrl.getUrl();
                            blocks.add(
                                new AnthropicContentBlock("image", source)
                            );
                        }
                        break;
                    default:
                        break;
                }
            }
            m.content = blocks;
        } else {
            m.content = msg.getContent();
        }
        return m;
    }

    private DeepSeekModels.ChatResponse fromAnthropicResponse(
        AnthropicResponse resp,
        String model
    ) {
        StringBuilder text = new StringBuilder();
        if (resp.content != null) {
            for (AnthropicContent block : resp.content) {
                if ("text".equals(block.type) && block.text != null) {
                    text.append(block.text);
                }
            }
        }

        DeepSeekModels.ChatMessage message = new DeepSeekModels.ChatMessage(
            "assistant",
            text.toString()
        );

        DeepSeekModels.ChatResponse.Choice choice =
            new DeepSeekModels.ChatResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason(resp.stop_reason);

        DeepSeekModels.ChatResponse response = new DeepSeekModels.ChatResponse();
        response.setId(resp.id);
        response.setModel(resp.model);
        response.setChoices(List.of(choice));

        if (resp.usage != null) {
            DeepSeekModels.ChatResponse.Usage usage =
                new DeepSeekModels.ChatResponse.Usage();
            usage.setPromptTokens(resp.usage.input_tokens);
            usage.setCompletionTokens(resp.usage.output_tokens);
            usage.setTotalTokens(
                resp.usage.input_tokens + resp.usage.output_tokens
            );
            response.setUsage(usage);
        }

        return response;
    }

    // -------------------------------------------------------------------------
    // SSE parsing for Anthropic's streaming format
    // -------------------------------------------------------------------------

    private void parseAnthropicSSE(
        java.io.InputStream inputStream,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream)
            )
        ) {
            String currentEvent = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event: ")) {
                    currentEvent = line.substring(7).trim();
                    continue;
                }
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    if ("message_stop".equals(currentEvent)) break;

                    try {
                        if (
                            "content_block_delta".equals(currentEvent) ||
                            "content_block_start".equals(currentEvent)
                        ) {
                            AnthropicStreamEvent event = objectMapper.readValue(
                                data,
                                AnthropicStreamEvent.class
                            );
                            if (
                                event.delta != null &&
                                "text_delta".equals(event.delta.type) &&
                                event.delta.text != null
                            ) {
                                DeepSeekModels.ChatStreamChunk chunk = buildStreamChunk(
                                    event.delta.text,
                                    null
                                );
                                onChunk.accept(chunk);
                            }
                            if (
                                event.content_block != null &&
                                "thinking".equals(event.content_block.type)
                            ) {
                                // Thinking block started — skip until text_delta
                            }
                        } else if (
                            "content_block_delta".equals(currentEvent)
                        ) {
                            AnthropicStreamEvent event = objectMapper.readValue(
                                data,
                                AnthropicStreamEvent.class
                            );
                            if (
                                event.delta != null &&
                                "thinking_delta".equals(event.delta.type) &&
                                event.delta.thinking != null
                            ) {
                                DeepSeekModels.ChatStreamChunk chunk = buildStreamChunk(
                                    null,
                                    event.delta.thinking
                                );
                                onChunk.accept(chunk);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug(
                            "Skipping unparseable Anthropic SSE chunk: {}",
                            data
                        );
                    }
                }
            }
        }
    }

    private DeepSeekModels.ChatStreamChunk buildStreamChunk(
        String content,
        String reasoning
    ) {
        DeepSeekModels.ChatStreamChunk.Delta delta =
            new DeepSeekModels.ChatStreamChunk.Delta();
        delta.setContent(content);
        delta.setReasoningContent(reasoning);

        DeepSeekModels.ChatStreamChunk.StreamChoice choice =
            new DeepSeekModels.ChatStreamChunk.StreamChoice();
        choice.setIndex(0);
        choice.setDelta(delta);

        DeepSeekModels.ChatStreamChunk chunk =
            new DeepSeekModels.ChatStreamChunk();
        chunk.setChoices(List.of(choice));
        return chunk;
    }

    // -------------------------------------------------------------------------
    // Anthropic wire types
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicRequest {

        String model;
        int max_tokens;
        List<AnthropicMessage> messages;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String system;

        boolean stream;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Double temperature;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicMessage {

        String role;

        @com.fasterxml.jackson.annotation.JsonRawValue
        Object content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicContentBlock {

        String type;
        String text;

        @JsonProperty("source")
        AnthropicImageSource source;

        AnthropicContentBlock() {}

        AnthropicContentBlock(String type, String text) {
            this.type = type;
            this.text = text;
        }

        AnthropicContentBlock(String type, AnthropicImageSource source) {
            this.type = type;
            this.source = source;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicImageSource {

        String type;
        String url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicResponse {

        String id;
        String model;
        List<AnthropicContent> content;
        String stop_reason;
        AnthropicUsage usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicContent {

        String type;
        String text;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicUsage {

        int input_tokens;
        int output_tokens;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicModelList {

        List<AnthropicModelEntry> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicModelEntry {

        String id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicStreamEvent {

        AnthropicStreamDelta delta;
        AnthropicStreamContentBlock content_block;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicStreamDelta {

        String type;
        String text;
        String thinking;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicStreamContentBlock {

        String type;
    }
}
