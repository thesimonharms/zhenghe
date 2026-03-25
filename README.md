# ZhengHe

> **A clean, production-ready Java client library for the DeepSeek AI API.**

[![Java](https://img.shields.io/badge/Java-23-blue?logo=openjdk)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![GitHub Packages](https://img.shields.io/badge/GitHub%20Packages-v1.0.0-orange?logo=github)](https://github.com/thesimonharms/ZhengHe/packages)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()

---

### Why "ZhengHe"?

In the early 15th century, the Chinese admiral **Zheng He** (鄭和) led the largest naval expeditions in history — famously sailing all the way to **Java** (the island). Six centuries later, **DeepSeek** is a Chinese AI model making its own voyage to **Java** (the programming language).

ZhengHe is that voyage.

---

ZhengHe wraps the DeepSeek HTTP API in an idiomatic Java interface. It handles authentication, JSON serialization, chat history management, and error propagation — so you can focus on building, not plumbing.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage](#usage)
  - [Listing Available Models](#listing-available-models)
  - [Stateful Chat (with History)](#stateful-chat-with-history)
  - [Single-Turn Completion](#single-turn-completion)
  - [Configuring Token Limits](#configuring-token-limits)
  - [Clearing Chat History](#clearing-chat-history)
- [Configuration Reference](#configuration-reference)
- [API Reference](#api-reference)
- [Error Handling](#error-handling)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **Stateful conversations** — automatic chat history tracking across multiple turns
- **Single-turn completions** — fire-and-forget requests that don't affect history
- **Model listing** — enumerate available models at runtime
- **Configurable** — customise token limits per request or globally
- **Resource-safe** — `DeepSeekAPIClient` implements `Closeable`
- **Logging** — SLF4J bridging; bring your own backend (Logback, Log4j 2, etc.)
- **Resilient** — retries on connection failure; generous default timeouts

---

## Requirements

- Java 23+
- Maven 3.8+ (or Gradle)
- A [DeepSeek API key](https://platform.deepseek.com/)

---

## Installation

### Maven

Add the GitHub Packages repository and dependency to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/thesimonharms/ZhengHe</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.simonharms</groupId>
    <artifactId>ZhengHe</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

> You will also need to add your GitHub credentials to `~/.m2/settings.xml` to authenticate with GitHub Packages.

### Gradle

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/thesimonharms/ZhengHe")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key")  ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'com.simonharms:ZhengHe:1.0.0'
}
```

### SLF4J Logging Backend

ZhengHe uses SLF4J for logging. Add your preferred binding:

```xml
<!-- Logback (recommended) -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.6</version>
</dependency>
```

---

## Quick Start

```java
import com.simonharms.zhenghe.DeepSeekService;
import com.simonharms.zhenghe.DeepSeekModels;

DeepSeekService service = new DeepSeekService("YOUR_API_KEY", "https://api.deepseek.com");

DeepSeekModels.ChatResponse response = service.sendChatRequest("What is the capital of France?", "deepseek-chat");
System.out.println(response.getMessage()); // "The capital of France is Paris."
```

---

## Usage

### Listing Available Models

```java
import com.simonharms.zhenghe.DeepSeekService;
import com.simonharms.zhenghe.DeepSeekModels;
import com.simonharms.zhenghe.DeepSeekAPIException;

DeepSeekService service = new DeepSeekService("YOUR_API_KEY", "https://api.deepseek.com");

try {
    service.getModels().forEach(model ->
        System.out.println(model.getId() + " (owned by: " + model.getOwnedBy() + ")")
    );
} catch (DeepSeekAPIException e) {
    System.err.println("Failed to list models: " + e.getMessage());
}
```

### Stateful Chat (with History)

Each call to `sendChatRequest` appends both the user message and the assistant reply to an in-memory history. The full history is sent with every subsequent request, giving the model context about previous turns.

```java
DeepSeekService service = new DeepSeekService("YOUR_API_KEY", "https://api.deepseek.com", 2048);
String model = "deepseek-chat";

try {
    // Turn 1
    String reply1 = service.sendChatRequest("Tell me about the Great Wall of China.", model).getMessage();
    System.out.println("Assistant: " + reply1);

    // Turn 2 — the model remembers the topic
    String reply2 = service.sendChatRequest("How long did it take to build?", model).getMessage();
    System.out.println("Assistant: " + reply2);

    // Turn 3
    String reply3 = service.sendChatRequest("What was I just asking you about?", model).getMessage();
    System.out.println("Assistant: " + reply3); // Still aware of the Great Wall
} catch (DeepSeekAPIException e) {
    System.err.println("API error: " + e.getMessage());
}
```

### Single-Turn Completion

`generateCompletion` sends a one-off request and does **not** modify the chat history. Useful for independent queries within a session.

```java
try {
    DeepSeekModels.ChatResponse response =
        service.generateCompletion("Summarise the Turing test in one sentence.", "deepseek-chat");
    System.out.println(response.getMessage());
} catch (DeepSeekAPIException e) {
    System.err.println("Error: " + e.getMessage());
}
```

### Configuring Token Limits

```java
// Set a global default used by all requests that don't specify a limit
service.setDefaultMaxTokens(4096);

// Override per request
service.sendChatRequest("Write a short poem.", "deepseek-chat", 256);
```

### Clearing Chat History

Start a fresh conversation without creating a new `DeepSeekService` instance:

```java
service.clearChatHistory();
```

You can also inspect the history at any time:

```java
service.getChatHistory().forEach(msg ->
    System.out.println(msg.getRole() + ": " + msg.getContent())
);
```

---

## Configuration Reference

### `DeepSeekService` Constructors

| Constructor | Description |
|---|---|
| `DeepSeekService(String apiKey, String baseUrl)` | Uses default max tokens (2048) |
| `DeepSeekService(String apiKey, String baseUrl, int defaultMaxTokens)` | Custom default token limit |

### Parameters

| Parameter | Type | Description |
|---|---|---|
| `apiKey` | `String` | Your DeepSeek API key |
| `baseUrl` | `String` | API base URL, e.g. `"https://api.deepseek.com"` |
| `defaultMaxTokens` | `int` | Default token limit per response (default: `2048`) |

---

## API Reference

### `DeepSeekService`

| Method | Description |
|---|---|
| `getModels()` | Returns all available models |
| `sendChatRequest(message, model)` | Sends a message; updates history |
| `sendChatRequest(message, model, maxTokens)` | Sends a message with a custom token limit |
| `generateCompletion(prompt, model)` | Stateless single-turn request; history unchanged |
| `generateCompletion(prompt, model, maxTokens)` | Stateless with custom token limit |
| `getChatHistory()` | Returns an unmodifiable view of the current history |
| `clearChatHistory()` | Clears all conversation history |
| `setDefaultMaxTokens(int)` | Updates the global default token limit |
| `getDefaultMaxTokens()` | Returns the current default token limit |

### `DeepSeekModels.ChatResponse`

| Method | Description |
|---|---|
| `getMessage()` | Returns the text content of the first choice |
| `getChoices()` | Returns all response choices |
| `getUsage()` | Returns token usage statistics |
| `getId()` | Returns the response ID |
| `getModel()` | Returns the model that generated the response |

---

## Error Handling

All API errors throw `DeepSeekAPIException` (a checked exception). Where applicable, the HTTP status code is available via `getStatusCode()`.

```java
try {
    service.sendChatRequest("Hello", "deepseek-chat");
} catch (DeepSeekAPIException e) {
    System.err.println("Message:     " + e.getMessage());
    System.err.println("Status code: " + e.getStatusCode()); // -1 if not an HTTP error
    System.err.println("Cause:       " + e.getCause());
}
```

`ChatResponse.getMessage()` throws `IllegalStateException` if the API returns a malformed response (empty choices, null content, etc.).

```java
try {
    String text = response.getMessage();
} catch (IllegalStateException e) {
    System.err.println("Unexpected response structure: " + e.getMessage());
}
```

---

## Contributing

Contributions are welcome! Please open an issue first to discuss significant changes.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Write tests for your changes
4. Ensure all tests pass: `mvn test`
5. Open a pull request

---

## License

Released under the [MIT License](https://opensource.org/licenses/MIT). See [LICENSE](LICENSE) for details.
