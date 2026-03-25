package com.simonharms.zhenghe;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekAPIClientTest {

    private MockWebServer server;
    private DeepSeekAPIClient client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        String baseUrl = server.url("").toString().replaceAll("/$", "");
        client = new DeepSeekAPIClient("test-key", baseUrl, new OkHttpClient(), mapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        client.close();
    }

    @Test
    void sendGetRequest_success_returnsDeserializedObject() throws Exception {
        String json = "{\"object\":\"list\",\"data\":[{\"id\":\"deepseek-chat\",\"object\":\"model\",\"owned_by\":\"deepseek\"}]}";
        server.enqueue(new MockResponse().setBody(json).setResponseCode(200)
                .addHeader("Content-Type", "application/json"));

        DeepSeekModels.ModelResponse response = client.sendGetRequest("/models", DeepSeekModels.ModelResponse.class);

        assertNotNull(response);
        assertEquals("list", response.getObject());
        assertEquals(1, response.getData().size());
        assertEquals("deepseek-chat", response.getData().get(0).getId());
    }

    @Test
    void sendGetRequest_setsAuthorizationHeader() throws Exception {
        String json = "{\"object\":\"list\",\"data\":[]}";
        server.enqueue(new MockResponse().setBody(json).setResponseCode(200)
                .addHeader("Content-Type", "application/json"));

        client.sendGetRequest("/models", DeepSeekModels.ModelResponse.class);

        RecordedRequest request = server.takeRequest();
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
    }

    @Test
    void sendGetRequest_nonSuccessCode_throwsIOException() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"Unauthorized\"}"));

        assertThrows(IOException.class, () ->
                client.sendGetRequest("/models", DeepSeekModels.ModelResponse.class));
    }

    @Test
    void sendGetRequest_serverError_throwsIOException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        assertThrows(IOException.class, () ->
                client.sendGetRequest("/models", DeepSeekModels.ModelResponse.class));
    }

    @Test
    void sendPostRequest_success_returnsDeserializedObject() throws Exception {
        String json = """
                {
                  "id": "resp-1",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "deepseek-chat",
                  "choices": [
                    {"index": 0, "finish_reason": "stop",
                     "message": {"role": "assistant", "content": "Hello!"}}
                  ],
                  "usage": {"completion_tokens": 5, "prompt_tokens": 10, "total_tokens": 15}
                }
                """;
        server.enqueue(new MockResponse().setBody(json).setResponseCode(200)
                .addHeader("Content-Type", "application/json"));

        java.util.List<DeepSeekModels.ChatMessage> messages = new java.util.ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "Hi"));
        DeepSeekModels.ChatRequest requestBody = new DeepSeekModels.ChatRequest("deepseek-chat", messages, 100);

        DeepSeekModels.ChatResponse response =
                client.sendPostRequest("/chat/completions", requestBody, DeepSeekModels.ChatResponse.class);

        assertNotNull(response);
        assertEquals("resp-1", response.getId());
        assertEquals("Hello!", response.getMessage());
    }

    @Test
    void sendPostRequest_setsCorrectHeaders() throws Exception {
        String json = """
                {"id":"r","object":"chat.completion","created":0,"model":"m",
                 "choices":[{"index":0,"finish_reason":"stop",
                   "message":{"role":"assistant","content":"ok"}}],
                 "usage":{"completion_tokens":1,"prompt_tokens":1,"total_tokens":2}}
                """;
        server.enqueue(new MockResponse().setBody(json).setResponseCode(200)
                .addHeader("Content-Type", "application/json"));

        java.util.List<DeepSeekModels.ChatMessage> messages = new java.util.ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "test"));
        client.sendPostRequest("/chat/completions",
                new DeepSeekModels.ChatRequest("deepseek-chat", messages, 10),
                DeepSeekModels.ChatResponse.class);

        RecordedRequest request = server.takeRequest();
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
        assertTrue(request.getHeader("Content-Type").startsWith("application/json"));
    }

    @Test
    void sendPostRequest_serverError_throwsIOException() {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate limited\"}"));

        java.util.List<DeepSeekModels.ChatMessage> messages = new java.util.ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "test"));
        DeepSeekModels.ChatRequest requestBody = new DeepSeekModels.ChatRequest("deepseek-chat", messages, 10);

        assertThrows(IOException.class, () ->
                client.sendPostRequest("/chat/completions", requestBody, DeepSeekModels.ChatResponse.class));
    }
}
