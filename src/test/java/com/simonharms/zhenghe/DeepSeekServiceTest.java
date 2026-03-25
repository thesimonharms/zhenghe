package com.simonharms.zhenghe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

        DeepSeekModels.ModelResponse response = new DeepSeekModels.ModelResponse();
        response.setData(List.of(model));

        when(mockClient.sendGetRequest("/models", DeepSeekModels.ModelResponse.class))
                .thenReturn(response);

        List<DeepSeekModels.ModelData> result = service.getModels();

        assertEquals(1, result.size());
        assertEquals("deepseek-chat", result.get(0).getId());
    }

    @Test
    void getModels_clientThrows_throwsDeepSeekAPIException() throws Exception {
        when(mockClient.sendGetRequest(anyString(), any()))
                .thenThrow(new IOException("network error"));

        assertThrows(DeepSeekAPIException.class, () -> service.getModels());
    }

    // --- sendChatRequest ---

    @Test
    void sendChatRequest_success_returnsResponse() throws Exception {
        DeepSeekModels.ChatResponse chatResponse = buildChatResponse("Hello!");
        when(mockClient.sendPostRequest(eq("/chat/completions"), any(), eq(DeepSeekModels.ChatResponse.class)))
                .thenReturn(chatResponse);

        DeepSeekModels.ChatResponse result = service.sendChatRequest("Hi", "deepseek-chat");

        assertNotNull(result);
        assertEquals("Hello!", result.getMessage());
    }

    @Test
    void sendChatRequest_addsUserMessageAndAssistantReplyToHistory() throws Exception {
        DeepSeekModels.ChatResponse chatResponse = buildChatResponse("Reply from model");
        when(mockClient.sendPostRequest(eq("/chat/completions"), any(), eq(DeepSeekModels.ChatResponse.class)))
                .thenReturn(chatResponse);

        service.sendChatRequest("User message", "deepseek-chat");

        List<DeepSeekModels.ChatMessage> history = service.getChatHistory();
        assertEquals(2, history.size());
        assertEquals("user", history.get(0).getRole());
        assertEquals("User message", history.get(0).getContent());
        assertEquals("assistant", history.get(1).getRole());
        assertEquals("Reply from model", history.get(1).getContent());
    }

    @Test
    void sendChatRequest_buildsRequestWithCorrectMaxTokens() throws Exception {
        DeepSeekModels.ChatResponse chatResponse = buildChatResponse("ok");
        when(mockClient.sendPostRequest(eq("/chat/completions"), any(), eq(DeepSeekModels.ChatResponse.class)))
                .thenReturn(chatResponse);

        service.sendChatRequest("Hello", "deepseek-chat", 512);

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
                ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(eq("/chat/completions"), captor.capture(),
                eq(DeepSeekModels.ChatResponse.class));

        assertEquals(512, captor.getValue().getMaxTokens());
    }

    @Test
    void sendChatRequest_clientThrows_throwsDeepSeekAPIException() throws Exception {
        when(mockClient.sendPostRequest(anyString(), any(), any()))
                .thenThrow(new IOException("timeout"));

        assertThrows(DeepSeekAPIException.class, () ->
                service.sendChatRequest("Hello", "deepseek-chat"));
    }

    @Test
    void sendChatRequest_multiTurn_accumulatesHistory() throws Exception {
        when(mockClient.sendPostRequest(eq("/chat/completions"), any(), eq(DeepSeekModels.ChatResponse.class)))
                .thenReturn(buildChatResponse("First reply"))
                .thenReturn(buildChatResponse("Second reply"));

        service.sendChatRequest("First question", "deepseek-chat");
        service.sendChatRequest("Second question", "deepseek-chat");

        assertEquals(4, service.getChatHistory().size());
    }

    // --- clearChatHistory ---

    @Test
    void clearChatHistory_emptiesHistory() throws Exception {
        when(mockClient.sendPostRequest(eq("/chat/completions"), any(), eq(DeepSeekModels.ChatResponse.class)))
                .thenReturn(buildChatResponse("Hello"));

        service.sendChatRequest("Hi", "deepseek-chat");
        assertFalse(service.getChatHistory().isEmpty());

        service.clearChatHistory();
        assertTrue(service.getChatHistory().isEmpty());
    }

    @Test
    void getChatHistory_returnsUnmodifiableList() throws Exception {
        when(mockClient.sendPostRequest(eq("/chat/completions"), any(), eq(DeepSeekModels.ChatResponse.class)))
                .thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("Hi", "deepseek-chat");
        List<DeepSeekModels.ChatMessage> history = service.getChatHistory();

        assertThrows(UnsupportedOperationException.class, () ->
                history.add(new DeepSeekModels.ChatMessage("user", "injected")));
    }

    // --- defaultMaxTokens ---

    @Test
    void defaultMaxTokens_canBeSetAndRetrieved() {
        service.setDefaultMaxTokens(4096);
        assertEquals(4096, service.getDefaultMaxTokens());
    }

    @Test
    void defaultMaxTokens_usedWhenNotSpecified() throws Exception {
        service.setDefaultMaxTokens(1000);
        when(mockClient.sendPostRequest(eq("/chat/completions"), any(), eq(DeepSeekModels.ChatResponse.class)))
                .thenReturn(buildChatResponse("ok"));

        service.sendChatRequest("test", "deepseek-chat");

        ArgumentCaptor<DeepSeekModels.ChatRequest> captor =
                ArgumentCaptor.forClass(DeepSeekModels.ChatRequest.class);
        verify(mockClient).sendPostRequest(anyString(), captor.capture(), any());

        assertEquals(1000, captor.getValue().getMaxTokens());
    }

    // --- generateCompletion ---

    @Test
    void generateCompletion_doesNotModifyChatHistory() throws Exception {
        when(mockClient.sendPostRequest(eq("/chat/completions"), any(), eq(DeepSeekModels.ChatResponse.class)))
                .thenReturn(buildChatResponse("result"));

        service.generateCompletion("What is 2+2?", "deepseek-chat");

        assertTrue(service.getChatHistory().isEmpty());
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
