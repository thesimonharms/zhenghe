ZhengHe
==========

ZhengHe is a Java Client for accessing the DeepSeek API.

Example Usage
================

```java
package com.example.test;

import java.util.List;
import com.simonharms.zhenghe.*;

public class Main {
    public static void main(String[] args) {
        // API key and base URL
        String apiKey = "YOUR API KEY";
        String baseUrl = "https://api.deepseek.com";

        // Create an instance of DeepSeekService with a custom default maxTokens value
        DeepSeekService service = new DeepSeekService(apiKey, baseUrl, 1000); // Default maxTokens = 1000
        // Example: Start a continuous chat
        String model = "deepseek-chat";

        // List available models
        try {
            List<DeepSeekModels.ModelData> modelsList = service.getModels();
            System.out.println(modelsList.toString());
        } catch (DeepSeekAPIException e) {
            System.err.println("API Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.err.println("Invalid Response: " + e.getMessage());
        }

        // First message
        try {
            String message1 = "Tell me the history of China";
            DeepSeekModels.ChatResponse response1 = service.sendChatRequest(message1, model);
            System.out.println("Chat Response 1: " + response1.getMessage());
        } catch (DeepSeekAPIException e) {
            // Handle API communication errors
            System.err.println("API Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Handle invalid response structure
            System.err.println("Invalid Response: " + e.getMessage());
        }

        // Second message (continuous chat)
        try {
            String message2 = "What are common foods in China?";
            DeepSeekModels.ChatResponse response2 = service.sendChatRequest(message2, model);
            System.out.println("Chat Response 2: " + response2.getMessage());
        } catch (DeepSeekAPIException e) {
            // Handle API communication errors
            System.err.println("API Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Handle invalid response structure
            System.err.println("Invalid Response: " + e.getMessage());
        }

        // Third message (continuous chat)
        try {
            String message3 = "What are the most recent developments in China?";
            DeepSeekModels.ChatResponse response3 = service.sendChatRequest(message3, model);
            System.out.println("Chat Response 3: " + response3.getMessage());
        } catch (DeepSeekAPIException e) {
            // Handle API communication errors
            System.err.println("API Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Handle invalid response structure
            System.err.println("Invalid Response: " + e.getMessage());
        }

        // Fourth message (continuous chat)
        try {
            String message4 = "What country was I just asking you about?";
            DeepSeekModels.ChatResponse response4 = service.sendChatRequest(message4, model);
            System.out.println("Chat Response 4: " + response4.getMessage());
        } catch (DeepSeekAPIException e) {
            // Handle API communication errors
            System.err.println("API Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Handle invalid response structure
            System.err.println("Invalid Response: " + e.getMessage());
        }

        // Clear the chat history
        service.clearChatHistory();
        System.out.println("Chat history cleared.");

        // Start a new chat
        try {
            String message5 = "What country was I just asking you about?";
            DeepSeekModels.ChatResponse response5 = service.sendChatRequest(message5, model);
            System.out.println("Chat Response 5: " + response5.getMessage());
        } catch (DeepSeekAPIException e) {
            // Handle API communication errors
            System.err.println("API Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Handle invalid response structure
            System.err.println("Invalid Response: " + e.getMessage());
        }
    }
}
```
