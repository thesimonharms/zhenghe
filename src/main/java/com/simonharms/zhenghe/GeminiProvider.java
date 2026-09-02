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
 * Provider for <a href="https://ai.google.dev/">Google Gemini</a>.
 *
 * <p>The Gemini API uses a different request/response format from
 * OpenAI-compatible providers. This class handles the conversion automatically.
 * Authentication uses the {@code x-goog-api-key} header.
 *
 * <p>Requires the {@code GEMINI_API_KEY} environment variable or an API key
 * passed to the constructor.
 */
public class GeminiProvider implements Provider {

    private static final Logger logger = LoggerFactory.getLogger(
        GeminiProvider.class
    );

    private static final String BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta";

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiProvider(String apiKey) {
        this(apiKey, BASE_URL);
    }

    public GeminiProvider(String apiKey, String baseUrl) {
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

    GeminiProvider(
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
        return "gemini";
    }

    @Override
    public DeepSeekModels.ChatResponse chat(
        DeepSeekModels.ChatRequest request
    ) throws ProviderException {
        try {
            Map<String, Object> body = toGeminiRequest(request);
            String json = objectMapper.writeValueAsString(body);
            String url =
                baseUrl + "/models/" + request.getModel() + ":generateContent";

            Request httpRequest = new Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
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
                        "Gemini API error: HTTP " + response.code(),
                        null,
                        response.code(),
                        "gemini"
                    );
                }

                GeminiResponse geminiResponse = objectMapper.readValue(
                    responseBody,
                    GeminiResponse.class
                );
                return fromGeminiResponse(
                    geminiResponse,
                    request.getModel()
                );
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Gemini request failed: " + e.getMessage(),
                e,
                -1,
                "gemini"
            );
        }
    }

    @Override
    public void streamChat(
        DeepSeekModels.ChatRequest request,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws ProviderException {
        try {
            Map<String, Object> body = toGeminiRequest(request);
            String json = objectMapper.writeValueAsString(body);
            String url =
                baseUrl +
                "/models/" +
                request.getModel() +
                ":streamGenerateContent?alt=sse";

            Request httpRequest = new Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
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
                        "Gemini streaming error: HTTP " +
                            response.code() +
                            " — " +
                        errorBody,
                        null,
                        response.code(),
                        "gemini"
                    );
                }

                parseGeminiSSE(
                    response.body().byteStream(),
                    request.getModel(),
                    onChunk
                );
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Gemini streaming request failed: " + e.getMessage(),
                e,
                -1,
                "gemini"
            );
        }
    }

    @Override
    public List<ModelInfo> listModels() throws ProviderException {
        try {
            Request httpRequest = new Request.Builder()
                .url(baseUrl + "/models?key=" + apiKey)
                .header("Accept", "application/json")
                .get()
                .build();

            try (
                Response response = httpClient.newCall(httpRequest).execute()
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new ProviderException(
                        "Gemini models error: HTTP " + response.code(),
                        null,
                        response.code(),
                        "gemini"
                    );
                }

                String responseBody = response.body().string();
                GeminiModelList list = objectMapper.readValue(
                    responseBody,
                    GeminiModelList.class
                );
                List<ModelInfo> result = new ArrayList<>();
                if (list.models != null) {
                    for (GeminiModelEntry entry : list.models) {
                        String id = entry.name != null
                            ? entry.name.replaceFirst("^models/", "")
                            : null;
                        result.add(
                            new ModelInfo(
                                id,
                                "google",
                                entry.inputTokenLimit,
                                entry.displayName
                            )
                        );
                    }
                }
                return result;
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Gemini models request failed: " + e.getMessage(),
                e,
                -1,
                "gemini"
            );
        }
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    // -------------------------------------------------------------------------
    // Request conversion
    // -------------------------------------------------------------------------

    private Map<String, Object> toGeminiRequest(DeepSeekModels.ChatRequest request) {
        Map<String, Object> body = new HashMap<>();

        // System instruction
        List<String> systemTexts = new ArrayList<>();
        for (DeepSeekModels.ChatMessage msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) {
                systemTexts.add(msg.getContent());
            }
        }
        if (!systemTexts.isEmpty()) {
            Map<String, Object> sysInst = new HashMap<>();
            sysInst.put("parts", List.of(Map.of("text", String.join("\n", systemTexts))));
            body.put("systemInstruction", sysInst);
        }

        // Contents
        List<Map<String, Object>> contents = new ArrayList<>();
        for (DeepSeekModels.ChatMessage msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) continue;
            contents.add(toGeminiContent(msg));
        }
        body.put("contents", contents);

        // Generation config
        Map<String, Object> genConfig = new HashMap<>();
        if (request.getMaxTokens() > 0) {
            genConfig.put("maxOutputTokens", request.getMaxTokens());
        }
        if (request.getTemperature() != 1.0) {
            genConfig.put("temperature", request.getTemperature());
        }
        if (!genConfig.isEmpty()) {
            body.put("generationConfig", genConfig);
        }

        return body;
    }

    private Map<String, Object> toGeminiContent(DeepSeekModels.ChatMessage msg) {
        Map<String, Object> content = new HashMap<>();
        String role = "user".equals(msg.getRole()) ? "user" : "model";
        content.put("role", role);

        List<Map<String, Object>> parts = new ArrayList<>();

        List<DeepSeekModels.ContentPart> contentParts = msg.getContentParts();
        if (contentParts != null) {
            for (DeepSeekModels.ContentPart part : contentParts) {
                switch (part.getType()) {
                    case "text":
                        parts.add(Map.of("text", part.getText()));
                        break;
                    case "image_url":
                        DeepSeekModels.ContentPart.ImageUrl imgUrl =
                            part.getImageUrl();
                        if (imgUrl != null && imgUrl.getUrl() != null) {
                            String url = imgUrl.getUrl();
                            if (url.startsWith("data:")) {
                                // Inline base64
                                String[] parts_split = url.split(",", 2);
                                if (parts_split.length == 2) {
                                    String mimeType = url
                                        .substring(5, url.indexOf(';'))
                                        .replace("data:", "");
                                    parts.add(
                                        Map.of(
                                            "inlineData",
                                            Map.of(
                                                "mimeType",
                                                mimeType,
                                                "data",
                                                parts_split[1]
                                            )
                                        )
                                    );
                                }
                            } else {
                                parts.add(
                                    Map.of(
                                        "fileData",
                                        Map.of(
                                            "mimeType",
                                            "image/jpeg",
                                            "fileUri",
                                            url
                                        )
                                    )
                                );
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        } else {
            parts.add(Map.of("text", msg.getContent()));
        }

        content.put("parts", parts);
        return content;
    }

    // -------------------------------------------------------------------------
    // Response conversion
    // -------------------------------------------------------------------------

    private DeepSeekModels.ChatResponse fromGeminiResponse(
        GeminiResponse resp,
        String model
    ) {
        StringBuilder text = new StringBuilder();
        if (resp.candidates != null && !resp.candidates.isEmpty()) {
            GeminiCandidate candidate = resp.candidates.get(0);
            if (
                candidate.content != null && candidate.content.parts != null
            ) {
                for (GeminiResponsePart part : candidate.content.parts) {
                    if (part.text != null) text.append(part.text);
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
        choice.setFinishReason(
            resp.candidates != null && !resp.candidates.isEmpty()
                ? resp.candidates.get(0).finishReason
                : null
        );

        DeepSeekModels.ChatResponse response = new DeepSeekModels.ChatResponse();
        response.setId("");
        response.setModel(model);
        response.setChoices(List.of(choice));

        if (resp.usageMetadata != null) {
            DeepSeekModels.ChatResponse.Usage usage =
                new DeepSeekModels.ChatResponse.Usage();
            usage.setPromptTokens(
                resp.usageMetadata.promptTokenCount != null
                    ? resp.usageMetadata.promptTokenCount
                    : 0
            );
            usage.setCompletionTokens(
                resp.usageMetadata.candidatesTokenCount != null
                    ? resp.usageMetadata.candidatesTokenCount
                    : 0
            );
            usage.setTotalTokens(
                resp.usageMetadata.totalTokenCount != null
                    ? resp.usageMetadata.totalTokenCount
                    : 0
            );
            response.setUsage(usage);
        }

        return response;
    }

    // -------------------------------------------------------------------------
    // SSE parsing for Gemini's streaming format
    // -------------------------------------------------------------------------

    private void parseGeminiSSE(
        java.io.InputStream inputStream,
        String model,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream)
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (!line.startsWith("data: ")) continue;

                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;

                try {
                    GeminiResponse geminiResp = objectMapper.readValue(
                        data,
                        GeminiResponse.class
                    );
                    if (
                        geminiResp.candidates != null &&
                        !geminiResp.candidates.isEmpty()
                    ) {
                        GeminiCandidate candidate =
                            geminiResp.candidates.get(0);
                        if (
                            candidate.content != null &&
                            candidate.content.parts != null
                        ) {
                            for (GeminiResponsePart part : candidate.content.parts) {
                                if (part.text != null && !part.text.isEmpty()) {
                                    DeepSeekModels.ChatStreamChunk.Delta delta =
                                        new DeepSeekModels.ChatStreamChunk.Delta();
                                    delta.setContent(part.text);

                                    DeepSeekModels.ChatStreamChunk.StreamChoice choice =
                                        new DeepSeekModels.ChatStreamChunk.StreamChoice();
                                    choice.setIndex(0);
                                    choice.setDelta(delta);

                                    DeepSeekModels.ChatStreamChunk chunk =
                                        new DeepSeekModels.ChatStreamChunk();
                                    chunk.setModel(model);
                                    chunk.setChoices(List.of(choice));
                                    onChunk.accept(chunk);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug(
                        "Skipping unparseable Gemini SSE chunk: {}",
                        data
                    );
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Gemini wire types
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiResponse {

        List<GeminiCandidate> candidates;

        @JsonProperty("usageMetadata")
        GeminiUsageMetadata usageMetadata;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiCandidate {

        GeminiResponseContent content;

        @JsonProperty("finishReason")
        String finishReason;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiResponseContent {

        List<GeminiResponsePart> parts;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiResponsePart {

        String text;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiUsageMetadata {

        @JsonProperty("promptTokenCount")
        Integer promptTokenCount;

        @JsonProperty("candidatesTokenCount")
        Integer candidatesTokenCount;

        @JsonProperty("totalTokenCount")
        Integer totalTokenCount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiModelList {

        List<GeminiModelEntry> models;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiModelEntry {

        String name;

        @JsonProperty("displayName")
        String displayName;

        @JsonProperty("inputTokenLimit")
        Integer inputTokenLimit;
    }
}
