package com.simonharms.zhenghe;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeepSeekModelsTest {

    // ------------------------------------------------------------------------
    // Model constants & resolution
    // ------------------------------------------------------------------------

    @Test
    void activeModelConstants_areCorrect() {
        assertEquals("deepseek-v4-flash", DeepSeekModels.DEEPSEEK_V4_FLASH);
        assertEquals("deepseek-v4-pro", DeepSeekModels.DEEPSEEK_V4_PRO);
    }

    @Test
    void resolveModel_returnsFlashForNull() {
        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_FLASH,
            DeepSeekModels.resolveModel(null)
        );
    }

    @Test
    void resolveModel_passesThroughActiveModels() {
        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_FLASH,
            DeepSeekModels.resolveModel(DeepSeekModels.DEEPSEEK_V4_FLASH)
        );
        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_PRO,
            DeepSeekModels.resolveModel(DeepSeekModels.DEEPSEEK_V4_PRO)
        );
    }

    @Test
    void resolveModel_mapsDeprecatedModelsToFlash() {
        for (String deprecated : DeepSeekModels.getDeprecatedModels()) {
            assertEquals(
                DeepSeekModels.DEEPSEEK_V4_FLASH,
                DeepSeekModels.resolveModel(deprecated),
                "Deprecated model '" + deprecated + "' should resolve to flash"
            );
        }
    }

    @Test
    void resolveModel_passesThroughUnknownModels() {
        assertEquals(
            "some-future-model",
            DeepSeekModels.resolveModel("some-future-model")
        );
        assertEquals(
            "custom-model",
            DeepSeekModels.resolveModel("custom-model")
        );
    }

    @Test
    void isDeprecated_returnsTrueForKnownDeprecatedModels() {
        assertTrue(DeepSeekModels.isDeprecated("deepseek-chat"));
        assertTrue(DeepSeekModels.isDeprecated("deepseek-reasoner"));
        assertTrue(DeepSeekModels.isDeprecated("deepseek-coder"));
    }

    @Test
    void isDeprecated_returnsFalseForActiveModels() {
        assertFalse(
            DeepSeekModels.isDeprecated(DeepSeekModels.DEEPSEEK_V4_FLASH)
        );
        assertFalse(
            DeepSeekModels.isDeprecated(DeepSeekModels.DEEPSEEK_V4_PRO)
        );
    }

    @Test
    void isDeprecated_returnsFalseForUnknownModels() {
        assertFalse(DeepSeekModels.isDeprecated("unknown-model"));
    }

    @Test
    void getDeprecatedModels_containsAllExpectedIds() {
        var models = DeepSeekModels.getDeprecatedModels();
        assertTrue(models.contains("deepseek-chat"));
        assertTrue(models.contains("deepseek-reasoner"));
        assertTrue(models.contains("deepseek-coder"));
        assertTrue(models.contains("deepseek-v2"));
        assertTrue(models.contains("deepseek-v2-chat"));
        assertTrue(models.contains("deepseek-v3"));
        assertTrue(models.contains("deepseek-v3-chat"));
        assertEquals(7, models.size());
    }

    @Test
    void getDeprecatedModels_returnsUnmodifiableSet() {
        assertThrows(UnsupportedOperationException.class, () ->
            DeepSeekModels.getDeprecatedModels().add("new-deprecated")
        );
    }

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(
                PropertyAccessor.FIELD,
                JsonAutoDetect.Visibility.ANY
            );
    }

    // --- ChatMessage ---

    @Test
    void chatMessage_serializesToJson() throws Exception {
        DeepSeekModels.ChatMessage msg = new DeepSeekModels.ChatMessage(
            "user",
            "Hello"
        );
        String json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"content\":\"Hello\""));
    }

    @Test
    void chatMessage_deserializesFromJson() throws Exception {
        String json = "{\"role\":\"assistant\",\"content\":\"Hi there\"}";
        DeepSeekModels.ChatMessage msg = mapper.readValue(
            json,
            DeepSeekModels.ChatMessage.class
        );

        assertEquals("assistant", msg.getRole());
        assertEquals("Hi there", msg.getContent());
    }

    // --- ChatRequest ---

    @Test
    void chatRequest_passesMessagesAsIs() {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "Hello"));

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(
            "deepseek-chat",
            messages,
            100
        );

        // ChatRequest is a plain data holder — it must not inject any extra messages
        assertEquals(1, request.getMessages().size());
        assertEquals("user", request.getMessages().get(0).getRole());
    }

    @Test
    void chatRequest_preservesSystemMessageWhenProvided() {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        messages.add(
            new DeepSeekModels.ChatMessage("system", "Custom instructions")
        );
        messages.add(new DeepSeekModels.ChatMessage("user", "Hello"));

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(
            "deepseek-chat",
            messages,
            100
        );

        assertEquals(2, request.getMessages().size());
        assertEquals("system", request.getMessages().get(0).getRole());
        assertEquals(
            "Custom instructions",
            request.getMessages().get(0).getContent()
        );
    }

    @Test
    void chatRequest_serializesMaxTokensWithCorrectKey() throws Exception {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "Hi"));

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(
            "deepseek-chat",
            messages,
            512
        );
        String json = mapper.writeValueAsString(request);

        assertTrue(
            json.contains("\"max_tokens\":512"),
            "Expected max_tokens key in JSON, got: " + json
        );
        assertFalse(
            json.contains("\"maxTokens\""),
            "maxTokens (camel case) must not appear in JSON"
        );
    }

    // --- ChatMessage (thinking mode) ---

    @Test
    void chatMessage_serializesReasoningContent() throws Exception {
        DeepSeekModels.ChatMessage msg = new DeepSeekModels.ChatMessage(
            "assistant",
            "visible text",
            "chain-of-thought reasoning"
        );
        String json = mapper.writeValueAsString(msg);

        assertTrue(
            json.contains(
                "\"reasoning_content\":\"chain-of-thought reasoning\""
            )
        );
        assertTrue(json.contains("\"content\":\"visible text\""));
        assertTrue(json.contains("\"role\":\"assistant\""));
    }

    @Test
    void chatMessage_omitsReasoningContentWhenNull() throws Exception {
        DeepSeekModels.ChatMessage msg = new DeepSeekModels.ChatMessage(
            "user",
            "Hello"
        );
        String json = mapper.writeValueAsString(msg);

        assertFalse(json.contains("reasoning_content"));
        assertTrue(json.contains("\"content\":\"Hello\""));
    }

    @Test
    void chatMessage_deserializesReasoningContent() throws Exception {
        String json =
            "{\"role\":\"assistant\",\"content\":\"Hi\",\"reasoning_content\":\"I thought about it\"}";
        DeepSeekModels.ChatMessage msg = mapper.readValue(
            json,
            DeepSeekModels.ChatMessage.class
        );

        assertEquals("assistant", msg.getRole());
        assertEquals("Hi", msg.getContent());
        assertEquals("I thought about it", msg.getReasoningContent());
    }

    // --- ChatRequest (thinking mode) ---

    @Test
    void chatRequest_serializesThinkingConfig() throws Exception {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "Hi"));

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(
            "deepseek-v4-pro",
            messages,
            100
        );
        request.setThinking(
            new DeepSeekModels.ChatRequest.ThinkingConfig("enabled")
        );
        request.setReasoningEffort("high");

        String json = mapper.writeValueAsString(request);

        assertTrue(
            json.contains("\"thinking\":{\"type\":\"enabled\"}"),
            "Expected thinking config in JSON: " + json
        );
        assertTrue(
            json.contains("\"reasoning_effort\":\"high\""),
            "Expected reasoning_effort in JSON: " + json
        );
    }

    @Test
    void chatRequest_omitsThinkingConfigWhenNull() throws Exception {
        List<DeepSeekModels.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekModels.ChatMessage("user", "Hi"));

        DeepSeekModels.ChatRequest request = new DeepSeekModels.ChatRequest(
            "deepseek-chat",
            messages,
            100
        );
        // thinking and reasoningEffort left null

        String json = mapper.writeValueAsString(request);
        assertFalse(
            json.contains("\"thinking\""),
            "JSON should not contain thinking when null: " + json
        );
        assertFalse(
            json.contains("\"reasoning_effort\""),
            "JSON should not contain reasoning_effort when null: " + json
        );
    }

    // --- ChatResponse ---

    @Test
    void chatResponse_getMessage_returnsContent() {
        DeepSeekModels.ChatResponse response = buildChatResponse(
            "Hello from model"
        );
        assertEquals("Hello from model", response.getMessage());
    }

    @Test
    void chatResponse_getMessage_throwsOnEmptyChoices() {
        DeepSeekModels.ChatResponse response =
            new DeepSeekModels.ChatResponse();
        response.setChoices(new ArrayList<>());

        assertThrows(IllegalStateException.class, response::getMessage);
    }

    @Test
    void chatResponse_toString_doesNotThrowOnEmptyChoices() {
        DeepSeekModels.ChatResponse response =
            new DeepSeekModels.ChatResponse();
        response.setChoices(new ArrayList<>());

        assertDoesNotThrow(response::toString);
    }

    // --- ModelResponse ---

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

        DeepSeekModels.ModelResponse response = mapper.readValue(
            json,
            DeepSeekModels.ModelResponse.class
        );

        assertEquals("list", response.getObject());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("deepseek-chat", response.getData().get(0).getId());
        assertEquals("deepseek", response.getData().get(0).getOwnedBy());
    }

    @Test
    void modelResponse_ignoresUnknownFields() {
        String json = """
            {
              "object": "list",
              "data": [],
              "some_future_field": "value"
            }
            """;

        assertDoesNotThrow(() ->
            mapper.readValue(json, DeepSeekModels.ModelResponse.class)
        );
    }

    // --- ChatStreamChunk ---

    @Test
    void chatStreamChunk_getContent_returnsContentDelta() throws Exception {
        String json = """
            {
              "id": "chunk-1",
              "model": "deepseek-chat",
              "choices": [
                {"index": 0, "finish_reason": null,
                 "delta": {"role": "assistant", "content": "Hello"}}
              ]
            }
            """;

        DeepSeekModels.ChatStreamChunk chunk = mapper.readValue(
            json,
            DeepSeekModels.ChatStreamChunk.class
        );
        assertEquals("Hello", chunk.getContent());
    }

    @Test
    void chatStreamChunk_getContent_returnsNullOnFinishChunk()
        throws Exception {
        String json = """
            {
              "id": "chunk-end",
              "model": "deepseek-chat",
              "choices": [
                {"index": 0, "finish_reason": "stop", "delta": {}}
              ]
            }
            """;

        DeepSeekModels.ChatStreamChunk chunk = mapper.readValue(
            json,
            DeepSeekModels.ChatStreamChunk.class
        );
        assertNull(chunk.getContent());
    }

    @Test
    void chatStreamChunk_getContent_returnsNullOnEmptyChoices() {
        DeepSeekModels.ChatStreamChunk chunk =
            new DeepSeekModels.ChatStreamChunk();
        assertNull(chunk.getContent());
    }

    // --- ChatStreamChunk (thinking mode) ---

    @Test
    void chatStreamChunk_getReasoningContent_returnsReasoningDelta()
        throws Exception {
        String json = """
            {
              "id": "chunk-r1",
              "model": "deepseek-v4-pro",
              "choices": [
                {"index": 0, "finish_reason": null,
                 "delta": {"role": "assistant", "reasoning_content": "Let me calculate..."}}
              ]
            }
            """;

        DeepSeekModels.ChatStreamChunk chunk = mapper.readValue(
            json,
            DeepSeekModels.ChatStreamChunk.class
        );
        assertEquals("Let me calculate...", chunk.getReasoningContent());
        assertNull(
            chunk.getContent(),
            "Content should be null when only reasoning is present"
        );
    }

    @Test
    void chatStreamChunk_deltaSerializesReasoningContent() throws Exception {
        DeepSeekModels.ChatStreamChunk.Delta delta =
            new DeepSeekModels.ChatStreamChunk.Delta();
        delta.setReasoningContent("thinking step");
        delta.setContent("final answer");

        String json = mapper.writeValueAsString(delta);

        assertTrue(json.contains("\"reasoning_content\":\"thinking step\""));
        assertTrue(json.contains("\"content\":\"final answer\""));
    }

    @Test
    void chatStreamChunk_getReasoningContent_returnsNullWhenNoReasoning()
        throws Exception {
        String json = """
            {
              "id": "chunk-c1",
              "model": "deepseek-v4-pro",
              "choices": [
                {"index": 0, "finish_reason": null,
                 "delta": {"content": "Hello"}}
              ]
            }
            """;

        DeepSeekModels.ChatStreamChunk chunk = mapper.readValue(
            json,
            DeepSeekModels.ChatStreamChunk.class
        );
        assertEquals("Hello", chunk.getContent());
        assertNull(chunk.getReasoningContent());
    }

    @Test
    void chatStreamChunk_getReasoningContent_returnsNullOnEmptyChoices() {
        DeepSeekModels.ChatStreamChunk chunk =
            new DeepSeekModels.ChatStreamChunk();
        assertNull(chunk.getReasoningContent());
    }

    // --- helpers ---

    private DeepSeekModels.ChatResponse buildChatResponse(String content) {
        DeepSeekModels.ChatMessage msg = new DeepSeekModels.ChatMessage(
            "assistant",
            content
        );

        DeepSeekModels.ChatResponse.Choice choice =
            new DeepSeekModels.ChatResponse.Choice();
        choice.setMessage(msg);
        choice.setFinishReason("stop");
        choice.setIndex(0);

        List<DeepSeekModels.ChatResponse.Choice> choices = new ArrayList<>();
        choices.add(choice);

        DeepSeekModels.ChatResponse response =
            new DeepSeekModels.ChatResponse();
        response.setId("test-id");
        response.setModel("deepseek-chat");
        response.setChoices(choices);
        return response;
    }
}
