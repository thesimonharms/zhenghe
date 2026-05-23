package com.simonharms.zhenghe;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DeepSeekServiceTest {

    private DeepSeekAPIClient mockClient;
    private DeepSeekService service;

    @BeforeEach
    void setUp() {
        mockClient = Mockito.mock(DeepSeekAPIClient.class);
        service = new DeepSeekService(mockClient, 2048);
    }

    // --- getModels ---

    @Test
    void getModels_returnsListFromClient() throws Exception {
        DeepSeekModels.ModelData model = new DeepSeekModels.ModelData();
        model.setId("deepseek-chat");
        model.setOwnedBy("deepseek");

        DeepSeekModels.ModelResponse response =
            new DeepSeekModels.ModelResponse();
        response.setData(List.of(model));

        when(
            mockClient.sendGetRequest(
                "/models",
                DeepSeekModels.ModelResponse.class
            )
        ).thenReturn(response);

        List<DeepSeekModels.ModelData> result = service.getModels();

        assertEquals(1, result.size());
        assertEquals("deepseek-chat", result.get(0).getId());
    }

    @Test
    void getModels_clientThrows_throwsDeepSeekAPIException() throws Exception {
        when(mockClient.sendGetRequest(anyString(), any())).thenThrow(
            new IOException("network error")
        );

        assertThrows(DeepSeekAPIException.class, () -> service.getModels());
    }

    // --- Model resolution ---

    @Test
    void sendChatRequest_resolvesDeprecatedModel() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("Hello", "deepseek-chat");

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(
            eq("/chat/completions"),
            captor.capture(),
            eq(DeepSeekModels.ChatResponse.class)
        );

        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_FLASH,
            captor.getValue().getModel()
        );
    }

    @Test
    void sendChatRequest_resolvesNullModelToFlash() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("Hello", null);

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(
            eq("/chat/completions"),
            captor.capture(),
            eq(DeepSeekModels.ChatResponse.class)
        );

        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_FLASH,
            captor.getValue().getModel()
        );
    }

    @Test
    void sendChatRequest_passesThroughActiveModels() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("Hello", DeepSeekModels.DEEPSEEK_V4_FLASH);
        service.sendChatRequest("Hi", DeepSeekModels.DEEPSEEK_V4_PRO);

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient, times(2)).sendPostRequest(
            eq("/chat/completions"),
            captor.capture(),
            eq(DeepSeekModels.ChatResponse.class)
        );

        List<DeepSeekModels.ChatRequest> requests = captor.getAllValues();
        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_FLASH,
            requests.get(0).getModel()
        );
        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_PRO,
            requests.get(1).getModel()
        );
    }

    @Test
    void generateCompletion_resolvesDeprecatedModel() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("result"));

        service.generateCompletion("What is 2+2?", "deepseek-reasoner");

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(
            eq("/chat/completions"),
            captor.capture(),
            eq(DeepSeekModels.ChatResponse.class)
        );

        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_FLASH,
            captor.getValue().getModel()
        );
    }

    @Test
    void streamChatRequest_resolvesDeprecatedModel() throws Exception {
        doNothing()
            .when(mockClient)
            .sendStreamingPostRequest(anyString(), any(), any());

        service.streamChatRequest("Hi", "deepseek-coder", 100, t -> {});

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendStreamingPostRequest(
            anyString(),
            captor.capture(),
            any()
        );

        assertEquals(
            DeepSeekModels.DEEPSEEK_V4_FLASH,
            captor.getValue().getModel()
        );
    }

    // --- sendChatRequest ---

    @Test
    void sendChatRequest_success_returnsResponse() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("Hello!"));

        DeepSeekModels.ChatResponse result = service.sendChatRequest(
            "Hi",
            "deepseek-chat"
        );

        assertNotNull(result);
        assertEquals("Hello!", result.getMessage());
    }

    @Test
    void sendChatRequest_addsUserMessageAndAssistantReplyToHistory()
        throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("Reply from model"));

        service.sendChatRequest("User message", "deepseek-chat");

        List<DeepSeekModels.ChatMessage> history = service.getChatHistory();
        assertEquals(2, history.size());
        assertEquals("user", history.get(0).getRole());
        assertEquals("User message", history.get(0).getContent());
        assertEquals("assistant", history.get(1).getRole());
        assertEquals("Reply from model", history.get(1).getContent());
    }

    @Test
    void sendChatRequest_capturesReasoningContentInHistory() throws Exception {
        DeepSeekModels.ChatResponse response = buildChatResponseWithReasoning(
            "Final answer",
            "Chain of thought reasoning"
        );
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(response);

        service.sendChatRequest("Solve this", "deepseek-v4-pro");

        List<DeepSeekModels.ChatMessage> history = service.getChatHistory();
        assertEquals(2, history.size());
        assertEquals("assistant", history.get(1).getRole());
        assertEquals("Final answer", history.get(1).getContent());
        assertEquals(
            "Chain of thought reasoning",
            history.get(1).getReasoningContent()
        );
    }

    @Test
    void sendChatRequest_prependsSystemPromptToRequest() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("Hello", "deepseek-chat");

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(
            eq("/chat/completions"),
            captor.capture(),
            eq(DeepSeekModels.ChatResponse.class)
        );

        List<DeepSeekModels.ChatMessage> msgs = captor.getValue().getMessages();
        assertEquals("system", msgs.get(0).getRole());
        assertEquals(
            DeepSeekService.DEFAULT_SYSTEM_PROMPT,
            msgs.get(0).getContent()
        );
        assertEquals("user", msgs.get(1).getRole());
        assertEquals("Hello", msgs.get(1).getContent());
    }

    @Test
    void sendChatRequest_buildsRequestWithCorrectMaxTokens() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("Hello", "deepseek-chat", 512);

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(
            eq("/chat/completions"),
            captor.capture(),
            eq(DeepSeekModels.ChatResponse.class)
        );

        assertEquals(512, captor.getValue().getMaxTokens());
    }

    @Test
    void sendChatRequest_clientThrows_throwsDeepSeekAPIException()
        throws Exception {
        when(mockClient.sendPostRequest(anyString(), any(), any())).thenThrow(
            new IOException("timeout")
        );

        assertThrows(DeepSeekAPIException.class, () ->
            service.sendChatRequest("Hello", "deepseek-chat")
        );
    }

    @Test
    void sendChatRequest_multiTurn_accumulatesHistory() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        )
            .thenReturn(buildChatResponse("First reply"))
            .thenReturn(buildChatResponse("Second reply"));

        service.sendChatRequest("First question", "deepseek-chat");
        service.sendChatRequest("Second question", "deepseek-chat");

        assertEquals(4, service.getChatHistory().size());
    }

    // --- system prompt ---

    @Test
    void setSystemPrompt_changesPromptSentWithRequests() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.setSystemPrompt("You are a pirate");
        service.sendChatRequest("Hello", "deepseek-chat");

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(
            eq("/chat/completions"),
            captor.capture(),
            any()
        );

        assertEquals(
            "You are a pirate",
            captor.getValue().getMessages().get(0).getContent()
        );
    }

    @Test
    void setSystemPrompt_null_sendsNoSystemMessage() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.setSystemPrompt(null);
        service.sendChatRequest("Hello", "deepseek-chat");

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(
            eq("/chat/completions"),
            captor.capture(),
            any()
        );

        List<DeepSeekModels.ChatMessage> msgs = captor.getValue().getMessages();
        assertEquals(1, msgs.size());
        assertEquals("user", msgs.get(0).getRole());
    }

    @Test
    void getSystemPrompt_returnsDefault() {
        assertEquals(
            DeepSeekService.DEFAULT_SYSTEM_PROMPT,
            service.getSystemPrompt()
        );
    }

    // --- streamChatRequest ---

    @Test
    @SuppressWarnings("unchecked")
    void streamChatRequest_deliversTokensToConsumer() throws Exception {
        doAnswer(invocation -> {
            Consumer<DeepSeekModels.ChatStreamChunk> consumer =
                invocation.getArgument(2);
            consumer.accept(buildStreamChunk("Hello"));
            consumer.accept(buildStreamChunk(" world"));
            return null;
        })
            .when(mockClient)
            .sendStreamingPostRequest(eq("/chat/completions"), any(), any());

        StringBuilder collected = new StringBuilder();
        service.streamChatRequest(
            "Hi",
            "deepseek-chat",
            100,
            collected::append
        );

        assertEquals("Hello world", collected.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamChatRequest_addsFullResponseToHistory() throws Exception {
        doAnswer(invocation -> {
            Consumer<DeepSeekModels.ChatStreamChunk> consumer =
                invocation.getArgument(2);
            consumer.accept(buildStreamChunk("Full"));
            consumer.accept(buildStreamChunk(" reply"));
            return null;
        })
            .when(mockClient)
            .sendStreamingPostRequest(eq("/chat/completions"), any(), any());

        service.streamChatRequest("Hi", "deepseek-chat", 100, t -> {});

        List<DeepSeekModels.ChatMessage> history = service.getChatHistory();
        assertEquals(2, history.size());
        assertEquals("user", history.get(0).getRole());
        assertEquals("assistant", history.get(1).getRole());
        assertEquals("Full reply", history.get(1).getContent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamChatRequest_capturesReasoningContentInHistory()
        throws Exception {
        doAnswer(invocation -> {
            Consumer<DeepSeekModels.ChatStreamChunk> consumer =
                invocation.getArgument(2);
            // Reasoning chunks come before content chunks
            consumer.accept(
                buildReasoningStreamChunk("Let me think about this...")
            );
            consumer.accept(buildStreamChunk("The answer is 42."));
            return null;
        })
            .when(mockClient)
            .sendStreamingPostRequest(eq("/chat/completions"), any(), any());

        service.streamChatRequest(
            "What is the answer?",
            "deepseek-v4-pro",
            100,
            t -> {}
        );

        List<DeepSeekModels.ChatMessage> history = service.getChatHistory();
        assertEquals(2, history.size());
        assertEquals("assistant", history.get(1).getRole());
        assertEquals("The answer is 42.", history.get(1).getContent());
        assertEquals(
            "Let me think about this...",
            history.get(1).getReasoningContent()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamChatRequest_setsStreamTrueOnRequest() throws Exception {
        doNothing()
            .when(mockClient)
            .sendStreamingPostRequest(anyString(), any(), any());

        service.streamChatRequest("Hi", "deepseek-chat", 100, t -> {});

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
            ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendStreamingPostRequest(
            anyString(),
            captor.capture(),
            any()
        );

        assertTrue(captor.getValue().isStream());
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamChatRequest_clientThrows_throwsDeepSeekAPIException()
        throws Exception {
        doThrow(new IOException("stream error"))
            .when(mockClient)
            .sendStreamingPostRequest(anyString(), any(), any());

        assertThrows(DeepSeekAPIException.class, () ->
            service.streamChatRequest("Hi", "deepseek-chat", 100, t -> {})
        );
    }

    // --- clearChatHistory ---

    @Test
    void clearChatHistory_emptiesHistory() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("Hello"));

        service.sendChatRequest("Hi", "deepseek-chat");
        assertFalse(service.getChatHistory().isEmpty());

        service.clearChatHistory();
        assertTrue(service.getChatHistory().isEmpty());
    }

    @Test
    void getChatHistory_returnsUnmodifiableList() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("Hi", "deepseek-chat");
        List<DeepSeekModels.ChatMessage> history = service.getChatHistory();

        assertThrows(UnsupportedOperationException.class, () ->
            history.add(new DeepSeekModels.ChatMessage("user", "injected"))
        );
    }

    @Test
    void getChatHistory_returnsCopy_notLiveView() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("First", "deepseek-chat");
        List<DeepSeekModels.ChatMessage> snapshot = service.getChatHistory();
        int sizeBefore = snapshot.size();

        service.sendChatRequest("Second", "deepseek-chat");

        // The earlier snapshot must not have grown
        assertEquals(sizeBefore, snapshot.size());
    }

    // --- generateCompletion ---

    @Test
    void generateCompletion_doesNotModifyChatHistory() throws Exception {
        when(
            mockClient.sendPostRequest(
                eq("/chat/completions"),
                any(),
                eq(DeepSeekModels.ChatResponse.class)
            )
        ).thenReturn(buildChatResponse("result"));

        service.generateCompletion("What is 2+2?", "deepseek-chat");

        assertTrue(service.getChatHistory().isEmpty());
    }

    // --- defaultMaxTokens ---

    @Test
    void defaultMaxTokens_canBeSetAndRetrieved() {
        service.setDefaultMaxTokens(4096);
        assertEquals(4096, service.getDefaultMaxTokens());
    }

    // --- close ---

    @Test
    void close_delegatesToClient() {
        service.close();
        verify(mockClient).close();
    }

    // --- helpers ---

    private DeepSeekModels.ChatResponse buildChatResponse(String content) {
        return buildChatResponseWithReasoning(content, null);
    }

    private DeepSeekModels.ChatResponse buildChatResponseWithReasoning(
        String content,
        String reasoningContent
    ) {
        DeepSeekModels.ChatMessage msg = new DeepSeekModels.ChatMessage(
            "assistant",
            content,
            reasoningContent
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

    private DeepSeekModels.ChatStreamChunk buildStreamChunk(String content) {
        DeepSeekModels.ChatStreamChunk.Delta delta =
            new DeepSeekModels.ChatStreamChunk.Delta();
        delta.setContent(content);

        DeepSeekModels.ChatStreamChunk.StreamChoice choice =
            new DeepSeekModels.ChatStreamChunk.StreamChoice();
        choice.setDelta(delta);
        choice.setIndex(0);

        List<DeepSeekModels.ChatStreamChunk.StreamChoice> choices = List.of(
            choice
        );

        DeepSeekModels.ChatStreamChunk chunk =
            new DeepSeekModels.ChatStreamChunk();
        chunk.setChoices(choices);
        return chunk;
    }

    private DeepSeekModels.ChatStreamChunk buildReasoningStreamChunk(
        String reasoning
    ) {
        DeepSeekModels.ChatStreamChunk.Delta delta =
            new DeepSeekModels.ChatStreamChunk.Delta();
        delta.setReasoningContent(reasoning);

        DeepSeekModels.ChatStreamChunk.StreamChoice choice =
            new DeepSeekModels.ChatStreamChunk.StreamChoice();
        choice.setDelta(delta);
        choice.setIndex(0);

        List<DeepSeekModels.ChatStreamChunk.StreamChoice> choices = List.of(
            choice
        );

        DeepSeekModels.ChatStreamChunk chunk =
            new DeepSeekModels.ChatStreamChunk();
        chunk.setChoices(choices);
        return chunk;
    }
}
