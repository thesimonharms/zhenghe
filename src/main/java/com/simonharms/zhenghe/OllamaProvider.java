package com.simonharms.zhenghe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for <a href="https://ollama.com/">Ollama</a>.
 *
 * <p>Connects to a local Ollama server using its native {@code /api/chat} API.
 * Uses NDJSON streaming (not SSE). No API key required.
 *
 * <p>Default address: {@code http://localhost:11434}
 */
public class OllamaProvider implements Provider {

    private static final Logger logger = LoggerFactory.getLogger(
        OllamaProvider.class
    );

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaProvider() {
        this(DEFAULT_BASE_URL);
    }

    public OllamaProvider(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        this.objectMapper = OpenAICompatClient.defaultObjectMapper();
    }

    OllamaProvider(
        String baseUrl,
        OkHttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "ollama";
    }

    @Override
    public DeepSeekModels.ChatResponse chat(
        DeepSeekModels.ChatRequest request
    ) throws ProviderException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", request.getModel());
            body.put("messages", toOllamaMessages(request.getMessages()));
            body.put("stream", false);

            Map<String, Object> options = new HashMap<>();
            if (request.getMaxTokens() > 0) {
                options.put("num_predict", request.getMaxTokens());
            }
            if (request.getTemperature() != 1.0) {
                options.put("temperature", request.getTemperature());
            }
            if (request.getTopP() != 1.0) {
                options.put("top_p", request.getTopP());
            }
            if (!options.isEmpty()) {
                body.put("options", options);
            }

            String json = objectMapper.writeValueAsString(body);
            String url = baseUrl + "/api/chat";

            Request httpRequest = new Request.Builder()
                .url(url)
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
                        "Ollama API error: HTTP " + response.code(),
                        null,
                        response.code(),
                        "ollama"
                    );
                }

                OllamaChatResponse ollamaResp = objectMapper.readValue(
                    responseBody,
                    OllamaChatResponse.class
                );
                return fromOllamaResponse(ollamaResp);
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Ollama request failed: " + e.getMessage(),
                e,
                -1,
                "ollama"
            );
        }
    }

    @Override
    public void streamChat(
        DeepSeekModels.ChatRequest request,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws ProviderException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", request.getModel());
            body.put("messages", toOllamaMessages(request.getMessages()));
            body.put("stream", true);

            Map<String, Object> options = new HashMap<>();
            if (request.getMaxTokens() > 0) {
                options.put("num_predict", request.getMaxTokens());
            }
            if (request.getTemperature() != 1.0) {
                options.put("temperature", request.getTemperature());
            }
            if (request.getTopP() != 1.0) {
                options.put("top_p", request.getTopP());
            }
            if (!options.isEmpty()) {
                body.put("options", options);
            }

            String json = objectMapper.writeValueAsString(body);
            String url = baseUrl + "/api/chat";

            Request httpRequest = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
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
                        "Ollama streaming error: HTTP " +
                            response.code() +
                            " — " +
                        errorBody,
                        null,
                        response.code(),
                        "ollama"
                    );
                }

                parseOllamaNDJSON(response.body().byteStream(), onChunk);
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Ollama streaming request failed: " + e.getMessage(),
                e,
                -1,
                "ollama"
            );
        }
    }

    @Override
    public List<ModelInfo> listModels() throws ProviderException {
        try {
            String url = baseUrl + "/api/tags";

            Request httpRequest = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build();

            try (
                Response response = httpClient.newCall(httpRequest).execute()
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new ProviderException(
                        "Ollama models error: HTTP " + response.code(),
                        null,
                        response.code(),
                        "ollama"
                    );
                }

                String responseBody = response.body().string();
                OllamaModelList list = objectMapper.readValue(
                    responseBody,
                    OllamaModelList.class
                );
                List<ModelInfo> result = new ArrayList<>();
                if (list.models != null) {
                    for (OllamaModelEntry entry : list.models) {
                        result.add(
                            new ModelInfo(entry.name, "ollama")
                        );
                    }
                }
                return result;
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Ollama models request failed: " + e.getMessage(),
                e,
                -1,
                "ollama"
            );
        }
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<Map<String, Object>> toOllamaMessages(
        List<DeepSeekModels.ChatMessage> messages
    ) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DeepSeekModels.ChatMessage msg : messages) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            result.add(m);
        }
        return result;
    }

    private DeepSeekModels.ChatResponse fromOllamaResponse(OllamaChatResponse resp) {
        DeepSeekModels.ChatMessage message = new DeepSeekModels.ChatMessage(
            "assistant",
            resp.message != null ? resp.message.content : ""
        );

        DeepSeekModels.ChatResponse.Choice choice =
            new DeepSeekModels.ChatResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason(resp.done ? "stop" : null);

        DeepSeekModels.ChatResponse response = new DeepSeekModels.ChatResponse();
        response.setId("");
        response.setModel(resp.model);
        response.setChoices(List.of(choice));

        if (resp.promptEvalCount != null || resp.evalCount != null) {
            int prompt = resp.promptEvalCount != null
                ? resp.promptEvalCount
                : 0;
            int completion = resp.evalCount != null ? resp.evalCount : 0;
            DeepSeekModels.ChatResponse.Usage usage =
                new DeepSeekModels.ChatResponse.Usage();
            usage.setPromptTokens(prompt);
            usage.setCompletionTokens(completion);
            usage.setTotalTokens(prompt + completion);
            response.setUsage(usage);
        }

        return response;
    }

    // -------------------------------------------------------------------------
    // NDJSON parsing for Ollama (not SSE)
    // -------------------------------------------------------------------------

    private void parseOllamaNDJSON(
        java.io.InputStream inputStream,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream)
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                try {
                    OllamaStreamChunk ollamaChunk = objectMapper.readValue(
                        line,
                        OllamaStreamChunk.class
                    );

                    DeepSeekModels.ChatStreamChunk.Delta delta =
                        new DeepSeekModels.ChatStreamChunk.Delta();
                    if (
                        ollamaChunk.message != null &&
                        ollamaChunk.message.content != null &&
                        !ollamaChunk.message.content.isEmpty()
                    ) {
                        delta.setContent(ollamaChunk.message.content);
                    }

                    DeepSeekModels.ChatStreamChunk.StreamChoice choice =
                        new DeepSeekModels.ChatStreamChunk.StreamChoice();
                    choice.setIndex(0);
                    choice.setDelta(delta);
                    choice.setFinishReason(
                        ollamaChunk.done ? "stop" : null
                    );

                    DeepSeekModels.ChatStreamChunk chunk = new DeepSeekModels.ChatStreamChunk();
                    chunk.setModel(ollamaChunk.model);
                    chunk.setChoices(List.of(choice));
                    onChunk.accept(chunk);

                    if (ollamaChunk.done) break;
                } catch (Exception e) {
                    logger.debug(
                        "Skipping unparseable Ollama NDJSON line: {}",
                        line
                    );
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Wire types
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OllamaChatResponse {

        String model;
        OllamaResponseMessage message;
        boolean done;

        @JsonProperty("prompt_eval_count")
        Integer promptEvalCount;

        @JsonProperty("eval_count")
        Integer evalCount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OllamaResponseMessage {

        String role;
        String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OllamaStreamChunk {

        String model;
        OllamaResponseMessage message;
        boolean done;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OllamaModelList {

        List<OllamaModelEntry> models;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OllamaModelEntry {

        String name;
    }
}
