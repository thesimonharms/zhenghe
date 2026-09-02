package com.simonharms.zhenghe;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared HTTP client for OpenAI-compatible providers.
 *
 * <p>Handles Bearer token authentication (or no auth for local providers),
 * JSON serialization, SSE streaming, and error handling. Individual provider
 * classes wrap one of these and delegate common methods to it.
 *
 * <p>This class is intentionally low-level — it works with raw JSON strings
 * so that provider-specific request/response conversion can happen at the
 * provider layer.
 */
public class OpenAICompatClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(
        OpenAICompatClient.class
    );

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a client with API key authentication.
     *
     * @param apiKey  the API key for Bearer authentication
     * @param baseUrl the base URL of the API
     */
    public OpenAICompatClient(String apiKey, String baseUrl) {
        this(
            apiKey,
            baseUrl,
            new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build(),
            defaultObjectMapper()
        );
    }

    /**
     * Creates a client without API key authentication (for local providers).
     *
     * @param baseUrl the base URL of the API
     */
    public OpenAICompatClient(String baseUrl) {
        this(null, baseUrl);
    }

    /**
     * Package-private constructor for testing.
     */
    OpenAICompatClient(
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

    /**
     * Sends a POST request with a JSON body and returns the raw response JSON.
     *
     * @param endpoint the API endpoint path (appended to baseUrl)
     * @param jsonBody the JSON request body string
     * @return the raw JSON response body
     * @throws IOException if the request fails
     */
    public String chat(String endpoint, String jsonBody) throws IOException {
        String url = baseUrl + endpoint;
        logger.debug("POST {}", url);

        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json")
        );
        Request.Builder builder = new Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(body);
        applyAuth(builder);

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            String responseBody =
                response.body() != null ? response.body().string() : null;
            logger.debug("POST {} -> {}", url, response.code());

            if (response.isSuccessful() && responseBody != null) {
                return responseBody;
            } else {
                String errorBody =
                    responseBody != null ? responseBody : "(empty)";
                logger.error(
                    "POST {} failed: {} {}",
                    url,
                    response.code(),
                    response.message()
                );
                throw new ProviderHTTPException(
                    "POST request failed [" +
                        response.code() +
                        "]: " +
                        errorBody,
                    response.code(),
                    errorBody
                );
            }
        }
    }

    /**
     * Sends a streaming POST request and delivers parsed stream chunks to the consumer.
     *
     * @param endpoint the API endpoint path (appended to baseUrl)
     * @param jsonBody the JSON request body string (stream flag is set automatically)
     * @param onChunk  called once per parsed SSE chunk
     * @throws IOException if the request fails or the stream cannot be read
     */
    public void streamChat(
        String endpoint,
        String jsonBody,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws IOException {
        String url = baseUrl + endpoint;
        logger.debug("POST (streaming) {}", url);

        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json")
        );
        Request.Builder builder = new Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(body);
        applyAuth(builder);

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errorBody =
                    response.body() != null
                        ? response.body().string()
                        : "(empty)";
                logger.error(
                    "POST (streaming) {} failed: {} {}",
                    url,
                    response.code(),
                    response.message()
                );
                throw new ProviderHTTPException(
                    "Streaming request failed [" +
                        response.code() +
                        "]: " +
                        errorBody,
                    response.code(),
                    errorBody
                );
            }

            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream())
                )
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (!line.startsWith("data: ")) continue;

                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    try {
                        DeepSeekModels.ChatStreamChunk chunk =
                            objectMapper.readValue(
                                data,
                                DeepSeekModels.ChatStreamChunk.class
                            );
                        onChunk.accept(chunk);
                    } catch (Exception e) {
                        logger.debug(
                            "Skipping unparseable SSE chunk: {}",
                            data
                        );
                    }
                }
            }
        }
    }

    /**
     * Sends a GET request and returns the raw response JSON.
     *
     * @param endpoint the API endpoint path (appended to baseUrl)
     * @return the raw JSON response body
     * @throws IOException if the request fails
     */
    public String getModels(String endpoint) throws IOException {
        String url = baseUrl + endpoint;
        logger.debug("GET {}", url);

        Request.Builder builder = new Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get();
        applyAuth(builder);

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                logger.debug("GET {} -> {}", url, response.code());
                return body;
            } else {
                String body =
                    response.body() != null
                        ? response.body().string()
                        : "(empty)";
                logger.error(
                    "GET {} failed: {} {}",
                    url,
                    response.code(),
                    response.message()
                );
                throw new ProviderHTTPException(
                    "GET request failed [" + response.code() + "]: " + body,
                    response.code(),
                    body
                );
            }
        }
    }

    /**
     * Applies Bearer authentication to the request builder if an API key is set.
     */
    private void applyAuth(Request.Builder builder) {
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    OkHttpClient getHttpClient() {
        return httpClient;
    }

    ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
            .configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
            )
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * HTTP exception that carries the status code and response body.
     * Used internally by OpenAICompatClient; providers catch this
     * and wrap it in ProviderException.
     */
    static class ProviderHTTPException extends IOException {

        private final int statusCode;
        private final transient String responseBody;

        ProviderHTTPException(
            String message,
            int statusCode,
            String responseBody
        ) {
            super(message);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        int getStatusCode() {
            return statusCode;
        }

        String getResponseBody() {
            return responseBody;
        }
    }
}
