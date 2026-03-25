package com.simonharms.zhenghe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekModelsTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Test
    void chatMessage_serializesToJson() throws Exception {
        DeepSeekModels.ChatMessage msg = new DeepSeekModels.ChatMessage("user", "Hello");
        String json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"content\":\"Hello\""));
    }

    @Test
    void chatMessage_deserializesFromJson() throws Exception {
        String json = "{\"role\":\"assistant\",\"content\":\"Hi there\"}";
        DeepSeekModels.ChatMessage msg = mapper.readValue(json, DeepSeekModels.ChatMessage.class);

        assertEquals("assistant", msg.getRole());
        assertEquals("Hi there", msg.getContent());
    }

    @Test
    void chatRequest_addsSystemMessage_whenNonePresent() {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "Hello"));

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest("deepseek-chat", messages, 100);

        assertEquals(2, request.getMessages().size());
        assertEquals("system", request.getMessages().get(0).getRole());
        assertEquals("user", request.getMessages().get(1).getRole());
    }

    @Test
    void chatRequest_preservesExistingSystemMessage() {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("system", "Custom instructions"));
        messages.add(new DeepSeekModels.ChatMessage("user", "Hello"));

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest("deepseek-chat", messages, 100);

        assertEquals(2, request.getMessages().size());
        assertEquals("Custom instructions", request.getMessages().get(0).getContent());
    }

    @Test
    void chatRequest_serializesMaxTokensWithCorrectKey() throws Exception {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "Hi"));

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest("deepseek-chat", messages, 512);
        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"max_tokens\":512"), "Expected max_tokens key in JSON, got: " + json);
        assertFalse(json.contains("\"maxTokens\""), "maxTokens (camel case) must not appear in JSON");
    }

    @Test
    void chatResponse_getMessage_returnsContent() {
        DeepSeekModels.ChatResponse response = buildChatResponse("Hello from model");

        assertEquals("Hello from model", response.getMessage());
    }

    @Test
    void chatResponse_getMessage_throwsOnEmptyChoices() {
        DeepSeekModels.ChatResponse response = new DeepSeekModels.ChatResponse();
        response.setChoices(new ArrayList<>());

        assertThrows(IllegalStateException.class, response::getMessage);
    }

    @Test
    void chatResponse_toString_doesNotThrowOnEmptyChoices() {
        DeepSeekModels.ChatResponse response = new DeepSeekModels.ChatResponse();
        response.setChoices(new ArrayList<>());

        // Must not throw
        assertDoesNotThrow(response::toString);
    }

    @Test
    void modelResponse_deserializesFromJson() throws Exception {
        String json = """
                {
                  "object": "list",
                  "data": [
                    {"id": "deepseek-chat", "object": "model", "owned_by": "deepseek"}
                  ]
                }
                """;

        DeepSeekModels.ModelResponse response = mapper.readValue(json, DeepSeekModels.ModelResponse.class);

        assertEquals("list", response.getObject());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("deepseek-chat", response.getData().get(0).getId());
        assertEquals("deepseek", response.getData().get(0).getOwnedBy());
    }

    @Test
    void modelResponse_ignoresUnknownFields() throws Exception {
        String json = """
                {
                  "object": "list",
                  "data": [],
                  "some_future_field": "value"
                }
                """;

        assertDoesNotThrow(() -> mapper.readValue(json, DeepSeekModels.ModelResponse.class));
    }

    // --- helpers ---

    private DeepSeekModels.ChatResponse buildChatResponse(String content) {
        DeepSeekModels.ChatMessage msg = new DeepSeekModels.ChatMessage("assistant", content);

        DeepSeekModels.ChatResponse.Choice choice = new DeepSeekModels.ChatResponse.Choice();
        choice.setMessage(msg);
        choice.setFinishReason("stop");
        choice.setIndex(0);

        List<DeepSeekModels.ChatResponse.Choice> choices = new ArrayList<>();
        choices.add(choice);

        DeepSeekModels.ChatResponse response = new DeepSeekModels.ChatResponse();
        response.setId("test-id");
        response.setModel("deepseek-chat");
        response.setChoices(choices);
        return response;
    }
}
