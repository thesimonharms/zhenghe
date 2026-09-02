package com.simonharms.zhenghe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * Provider for <a href="https://developers.cloudflare.com/workers-ai/">Cloudflare Workers AI</a>.
 *
 * <p>Uses Cloudflare's native {@code /ai/run/} API. Models are identified by
 * their full path string (e.g. {@code @cf/meta/llama-3.1-8b-instruct}).
 * Requests are routed through your Cloudflare account.
 *
 * <p>Requires both {@code CLOUDFLARE_ACCOUNT_ID} and {@code CLOUDFLARE_API_TOKEN}.
 */
public class CloudflareProvider implements Provider {

    private static final Logger logger = LoggerFactory.getLogger(
        CloudflareProvider.class
    );

    private static final String BASE_URL = "https://api.cloudflare.com/client/v4";

    private final String accountId;
    private final String apiToken;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CloudflareProvider(String accountId, String apiToken) {
        this(accountId, apiToken, BASE_URL);
    }

    public CloudflareProvider(
        String accountId,
        String apiToken,
        String baseUrl
    ) {
        this.accountId = accountId;
        this.apiToken = apiToken;
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        this.objectMapper = OpenAICompatClient.defaultObjectMapper();
    }

    CloudflareProvider(
        String accountId,
        String apiToken,
        String baseUrl,
        OkHttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.accountId = accountId;
        this.apiToken = apiToken;
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "cloudflare";
    }

    @Override
    public DeepSeekModels.ChatResponse chat(
        DeepSeekModels.ChatRequest request
    ) throws ProviderException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("messages", toCfMessages(request.getMessages()));
            body.put("stream", false);
            if (request.getMaxTokens() > 0) {
                body.put("max_tokens", request.getMaxTokens());
            }
            if (request.getTemperature() != 1.0) {
                body.put("temperature", request.getTemperature());
            }

            String json = objectMapper.writeValueAsString(body);
            String url = runUrl(request.getModel());

            Request httpRequest = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiToken)
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
                        "Cloudflare API error: HTTP " + response.code(),
                        null,
                        response.code(),
                        "cloudflare"
                    );
                }

                CfResponse cfResp = objectMapper.readValue(
                    responseBody,
                    CfResponse.class
                );
                if (!cfResp.success) {
                    String msg = cfResp.errors != null
                        ? String.join("; ", cfResp.errors)
                        : "unknown error";
                    throw new ProviderException(
                        "Cloudflare API error: " + msg,
                        null,
                        200,
                        "cloudflare"
                    );
                }

                return fromCfResponse(
                    cfResp.result,
                    request.getModel()
                );
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Cloudflare request failed: " + e.getMessage(),
                e,
                -1,
                "cloudflare"
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
            body.put("messages", toCfMessages(request.getMessages()));
            body.put("stream", true);
            if (request.getMaxTokens() > 0) {
                body.put("max_tokens", request.getMaxTokens());
            }
            if (request.getTemperature() != 1.0) {
                body.put("temperature", request.getTemperature());
            }

            String json = objectMapper.writeValueAsString(body);
            String url = runUrl(request.getModel());

            Request httpRequest = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiToken)
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
                        "Cloudflare streaming error: HTTP " +
                            response.code() +
                            " — " +
                        errorBody,
                        null,
                        response.code(),
                        "cloudflare"
                    );
                }

                parseCfSSE(response.body().byteStream(), onChunk);
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Cloudflare streaming request failed: " + e.getMessage(),
                e,
                -1,
                "cloudflare"
            );
        }
    }

    @Override
    public List<ModelInfo> listModels() throws ProviderException {
        try {
            String url =
                baseUrl +
                "/accounts/" +
                accountId +
                "/ai/models/search?task=Text%20Generation";

            Request httpRequest = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiToken)
                .header("Accept", "application/json")
                .get()
                .build();

            try (
                Response response = httpClient.newCall(httpRequest).execute()
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new ProviderException(
                        "Cloudflare models error: HTTP " + response.code(),
                        null,
                        response.code(),
                        "cloudflare"
                    );
                }

                String responseBody = response.body().string();
                CfModelSearchResponse list = objectMapper.readValue(
                    responseBody,
                    CfModelSearchResponse.class
                );
                List<ModelInfo> result = new ArrayList<>();
                if (list.result != null) {
                    for (CfModel m : list.result) {
                        result.add(new ModelInfo(m.id, "cloudflare"));
                    }
                }
                return result;
            }
        } catch (ProviderException e) {
            throw e;
        } catch (IOException e) {
            throw new ProviderException(
                "Cloudflare models request failed: " + e.getMessage(),
                e,
                -1,
                "cloudflare"
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

    private String runUrl(String model) {
        return (
            baseUrl + "/accounts/" + accountId + "/ai/run/" + model
        );
    }

    private List<Map<String, String>> toCfMessages(
        List<DeepSeekModels.ChatMessage> messages
    ) {
        List<Map<String, String>> result = new ArrayList<>();
        for (DeepSeekModels.ChatMessage msg : messages) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            result.add(m);
        }
        return result;
    }

    private DeepSeekModels.ChatResponse fromCfResponse(String responseText, String model) {
        DeepSeekModels.ChatMessage message = new DeepSeekModels.ChatMessage(
            "assistant",
            responseText
        );

        DeepSeekModels.ChatResponse.Choice choice =
            new DeepSeekModels.ChatResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason("stop");

        DeepSeekModels.ChatResponse response = new DeepSeekModels.ChatResponse();
        response.setId("");
        response.setModel(model);
        response.setChoices(List.of(choice));
        return response;
    }

    private void parseCfSSE(
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
                if (line.isEmpty()) continue;
                if (!line.startsWith("data: ")) continue;

                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;

                try {
                    CfStreamChunk cfChunk = objectMapper.readValue(
                        data,
                        CfStreamChunk.class
                    );
                    if (cfChunk.response != null && !cfChunk.response.isEmpty()) {
                        DeepSeekModels.ChatStreamChunk.Delta delta =
                            new DeepSeekModels.ChatStreamChunk.Delta();
                        delta.setContent(cfChunk.response);

                        DeepSeekModels.ChatStreamChunk.StreamChoice choice =
                            new DeepSeekModels.ChatStreamChunk.StreamChoice();
                        choice.setIndex(0);
                        choice.setDelta(delta);

                        DeepSeekModels.ChatStreamChunk chunk = new DeepSeekModels.ChatStreamChunk();
                        chunk.setChoices(List.of(choice));
                        onChunk.accept(chunk);
                    }
                } catch (Exception e) {
                    logger.debug(
                        "Skipping unparseable Cloudflare SSE chunk: {}",
                        data
                    );
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Wire types
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CfResponse {

        String result;
        boolean success;
        List<String> errors;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CfStreamChunk {

        String response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CfModelSearchResponse {

        List<CfModel> result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CfModel {

        String id;
    }
}
