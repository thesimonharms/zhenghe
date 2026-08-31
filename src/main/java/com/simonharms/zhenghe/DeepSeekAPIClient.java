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
 * Low-level HTTP client for the DeepSeek API.
 * Handles authentication, JSON serialization, and response parsing.
 *
 * <p>Implements {@link Closeable} — call {@link #close()} when done to release
 * the underlying connection pool and executor service.
 */
public class DeepSeekAPIClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(
        DeepSeekAPIClient.class
    );

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new client with the given credentials.
     *
     * @param apiKey  the DeepSeek API key for Bearer authentication
     * @param baseUrl the base URL of the API (e.g., {@code "https://api.deepseek.com"})
     */
    public DeepSeekAPIClient(String apiKey, String baseUrl) {
        this(
            apiKey,
            baseUrl,
            new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build(),
            new ObjectMapper()
                .configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false
                )
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setVisibility(
                    PropertyAccessor.FIELD,
                    JsonAutoDetect.Visibility.ANY
                )
        );
    }

    /**
     * Package-private constructor for testing — accepts pre-configured HTTP client and mapper.
     */
    DeepSeekAPIClient(
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
     * Sends a GET request to the specified endpoint and deserializes the response.
     *
     * @param <T>          the expected response type
     * @param endpoint     the API endpoint path (appended to baseUrl)
     * @param responseType the class to deserialize the response into
     * @return the deserialized response object
     * @throws IOException if the request fails or the response cannot be processed
     */
    public <T> T sendGetRequest(String endpoint, Class<T> responseType)
        throws IOException {
        String url = baseUrl + endpoint;
        logger.debug("GET {}", url);

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Accept", "application/json")
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                logger.debug("GET {} -> {}", url, response.code());
                return objectMapper.readValue(body, responseType);
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
                throw new DeepSeekHTTPException(
                    "GET request failed [" + response.code() + "]: " + body,
                    response.code(),
                    body
                );
            }
        }
    }

    /**
     * Sends a POST request with a JSON body to the specified endpoint and deserializes the response.
     *
     * @param <T>          the expected response type
     * @param <R>          the request body type
     * @param endpoint     the API endpoint path (appended to baseUrl)
     * @param requestBody  the object to serialize as the JSON request body
     * @param responseType the class to deserialize the response into
     * @return the deserialized response object
     * @throws IOException if the request fails or the response cannot be processed
     */
    public <T, R> T sendPostRequest(
        String endpoint,
        R requestBody,
        Class<T> responseType
    ) throws IOException {
        String url = baseUrl + endpoint;
        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        logger.debug("POST {} payload: {}", url, jsonPayload);

        RequestBody body = RequestBody.create(
            jsonPayload,
            MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody =
                response.body() != null ? response.body().string() : null;
            logger.debug("POST {} -> {}", url, response.code());

            if (response.isSuccessful() && responseBody != null) {
                return objectMapper.readValue(responseBody, responseType);
            } else {
                logger.error(
                    "POST {} failed: {} {}\n{}",
                    url,
                    response.code(),
                    response.message(),
                    responseBody
                );
                throw new DeepSeekHTTPException(
                    "POST request failed [" +
                        response.code() +
                        "]: " +
                        responseBody,
                    response.code(),
                    responseBody
                );
            }
        }
    }

    /**
     * Sends a streaming POST request and delivers parsed stream chunks to the provided consumer.
     *
     * <p>The request body must have {@code "stream": true} set. Each parsed SSE chunk
     * is passed to {@code onChunk} as a {@link DeepSeekModels.ChatStreamChunk}.
     * The consumer is called on the calling thread and may be invoked many times before
     * this method returns.
     *
     * @param endpoint    the API endpoint path (appended to baseUrl)
     * @param requestBody the object to serialize as the JSON request body (should have stream=true)
     * @param onChunk     called once per parsed SSE chunk as it arrives from the API
     * @throws IOException if the request fails or the stream cannot be read
     */
    public void sendStreamingPostRequest(
        String endpoint,
        Object requestBody,
        Consumer<DeepSeekModels.ChatStreamChunk> onChunk
    ) throws IOException {
        String url = baseUrl + endpoint;
        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        logger.debug("POST (streaming) {}", url);

        RequestBody body = RequestBody.create(
            jsonPayload,
            MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
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
                throw new DeepSeekHTTPException(
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
     * Releases the underlying connection pool and thread pool.
     * Call this when the client is no longer needed.
     */
    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
