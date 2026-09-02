# ZhengHe

<p align="center">
  <img src="assets/logo.svg" alt="ZhengHe logo" width="180"/>
</p>

> **A multi-provider AI client library for Java — connecting your code to every major AI provider.**

[![Java](https://img.shields.io/badge/Java-23-blue?logo=openjdk)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

---

### Why "ZhengHe"?

In the early 15th century, Admiral **Zheng He** (鄭和) commanded the largest wooden fleet the world had ever seen. His colossal **treasure ships** sailed from China to the Persian Gulf, East Africa, and beyond — connecting civilizations that had never met. They carried silk, porcelain, and ideas across the Indian Ocean, opening the world to each other.

ZhengHe aspires to the same role: a vessel that carries your Java code to every major AI provider through a single unified interface, regardless of where those providers sail.

The sister library [**baochuan**](https://github.com/thesimonharms/baochuan) does the same for Rust — named after the treasure ships (宝船) that Admiral Zheng He famously commanded.

---

## Table of Contents

- [Supported Providers](#supported-providers)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Multi-Provider Usage](#multi-provider-usage)
  - [Provider-Agnostic Code](#provider-agnostic-code)
  - [Using the ProviderType Registry](#using-the-providertype-registry)
  - [Streaming](#streaming)
  - [Local Providers](#local-providers)
- [DeepSeek-Specific Features](#deepseek-specific-features)
  - [Stateful Chat (with History)](#stateful-chat-with-history)
  - [Single-Turn Completion](#single-turn-completion)
  - [Vision (Image Input)](#vision-image-input)
  - [Thinking Mode (Chain-of-Thought Reasoning)](#thinking-mode-chain-of-thought-reasoning)
  - [Active Models](#active-models)
- [API Key Security](#api-key-security)
- [API Reference](#api-reference)
- [Error Handling](#error-handling)
- [Contributing](#contributing)
- [License](#license)

---

## Supported Providers

| Provider | Chat | Streaming | Model List | Env Var |
|---|---|---|---|---|
| [OpenAI](https://platform.openai.com/) | ✅ | ✅ | ✅ | `OPENAI_API_KEY` |
| [Anthropic](https://www.anthropic.com/) | ✅ | ✅ | ✅ | `ANTHROPIC_API_KEY` |
| [Google Gemini](https://ai.google.dev/) | ✅ | ✅ | ✅ | `GEMINI_API_KEY` |
| [xAI Grok](https://docs.x.ai/) | ✅ | ✅ | ✅ | `XAI_API_KEY` |
| [Mistral](https://mistral.ai/) | ✅ | ✅ | ✅ | `MISTRAL_API_KEY` |
| [DeepSeek](https://platform.deepseek.com/) | ✅ | ✅ | ✅ | `DEEPSEEK_API_KEY` |
| [GitHub Copilot](https://github.com/features/copilot) | ✅ | ✅ | ✅ | `GITHUB_TOKEN` |
| [OpenRouter](https://openrouter.ai/) | ✅ | ✅ | ✅ | `OPENROUTER_API_KEY` |
| [Moonshot AI / Kimi](https://platform.kimi.ai/) | ✅ | ✅ | ✅ | `MOONSHOT_API_KEY` |
| [Perplexity](https://docs.perplexity.ai/) | ✅ | ✅ | ✅ | `PERPLEXITY_API_KEY` |
| [Nous Portal](https://portal.nousresearch.com/) | ✅ | ✅ | ✅ | `NOUS_API_KEY` |
| [Alibaba Qwen](https://dashscope.aliyun.com/) | ✅ | ✅ | ✅ | `DASHSCOPE_API_KEY` |
| [Groq](https://groq.com/) | ✅ | ✅ | ✅ | `GROQ_API_KEY` |
| [Together AI](https://www.together.ai/) | ✅ | ✅ | ✅ | `TOGETHER_API_KEY` |
| [Fireworks AI](https://fireworks.ai/) | ✅ | ✅ | ✅ | `FIREWORKS_API_KEY` |
| [Cerebras](https://inference.cerebras.ai/) | ✅ | ✅ | ✅ | `CEREBRAS_API_KEY` |
| [SambaNova](https://cloud.sambanova.ai/) | ✅ | ✅ | ✅ | `SAMBANOVA_API_KEY` |
| [Zhipu AI / Z.ai (GLM)](https://z.ai/) | ✅ | ✅ | ✅ | `ZHIPU_API_KEY` |
| [MiniMax](https://www.minimax.io/) | ✅ | ✅ | ✅ | `MINIMAX_API_KEY` |
| [Hugging Face](https://huggingface.co/docs/inference-providers) | ✅ | ✅ | ✅ | `HF_TOKEN` |
| [NVIDIA NIM](https://build.nvidia.com/) | ✅ | ✅ | ✅ | `NVIDIA_API_KEY` |
| [DeepInfra](https://deepinfra.com/) | ✅ | ✅ | ✅ | `DEEPINFRA_API_KEY` |
| [Cloudflare Workers AI](https://developers.cloudflare.com/workers-ai/) | ✅ | ✅ | ✅ | `CLOUDFLARE_ACCOUNT_ID` + `CLOUDFLARE_API_TOKEN` |
| [Ollama](https://ollama.com/) | ✅ | ✅ | ✅ | *(none)* |
| [LM Studio](https://lmstudio.ai/) | ✅ | ✅ | ✅ | *(none)* |
| [llama.cpp](https://github.com/ggerganov/llama.cpp) | ✅ | ✅ | ✅ | *(none)* |

---

## Features

- **Multi-provider** — swap providers without changing your business logic
- **27 providers** — OpenAI, Anthropic, Gemini, DeepSeek, Groq, Ollama, and 21 more
- **Provider-agnostic interface** — `Provider` trait works with any backend
- **Streaming** — real-time token delivery for all providers
- **Stateful conversations** — automatic chat history tracking (DeepSeek)
- **Vision support** — send images to vision-capable models (DeepSeek)
- **Thinking mode** — capture chain-of-thought reasoning (DeepSeek, Gemini, Kimi)
- **Typed errors** — HTTP status codes propagated through `ProviderException`
- **Configurable** — customise token limits per request or globally
- **Resource-safe** — all providers implement `Closeable`
- **Logging** — SLF4J bridging; bring your own backend
- **Resilient** — retries on connection failure; generous default timeouts
- **No SDK wrappers** — direct HTTP to each provider; no third-party SDK dependencies

---

## Requirements

- Java 23+
- Maven 3.8+ (or Gradle)
- An API key for your chosen provider (or no key for local providers like Ollama)

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
    <version>1.2.0</version>
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
    implementation 'com.simonharms:ZhengHe:1.2.0'
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

### Any provider

```java
import com.simonharms.zhenghe.*;

Provider provider = new GroqProvider(System.getenv("GROQ_API_KEY"));

ChatRequest request = new ChatRequest(
    "llama-3.3-70b-versatile",
    List.of(new ChatMessage("user", "What is the capital of France?")),
    512
);

ChatResponse response = provider.chat(request);
System.out.println(response.getMessage());
provider.close();
```

### Using the registry

```java
Provider provider = ProviderType.OPENAI.create(System.getenv("OPENAI_API_KEY"));

ChatRequest request = new ChatRequest(
    "gpt-4o",
    List.of(new ChatMessage("user", "Hello!")),
    256
);

ChatResponse response = provider.chat(request);
System.out.println(response.getMessage());
provider.close();
```

### Local providers (no API key)

```java
Provider ollama = ProviderType.OLLAMA.create();

ChatRequest request = new ChatRequest(
    "llama3.2",
    List.of(new ChatMessage("user", "Why is the sky blue?")),
    512
);

ChatResponse response = ollama.chat(request);
System.out.println(response.getMessage());
ollama.close();
```

---

## Multi-Provider Usage

### Provider-Agnostic Code

Write code that works with any provider:

```java
import com.simonharms.zhenghe.*;

public String ask(Provider provider, String model, String question) throws ProviderException {
    ChatRequest request = new ChatRequest(
        model,
        List.of(new ChatMessage("user", question)),
        512
    );
    return provider.chat(request).getMessage();
}

// Use with any provider
String answer1 = ask(new GroqProvider(groqKey), "llama-3.3-70b-versatile", "What is Java?");
String answer2 = ask(new OpenAIProvider(openaiKey), "gpt-4o", "What is Java?");
String answer3 = ask(new OllamaProvider(), "llama3.2", "What is Java?");
```

### Using the ProviderType Registry

The `ProviderType` enum provides a convenient factory for all supported providers:

```java
// Create from API key
Provider provider = ProviderType.GROQ.create(System.getenv("GROQ_API_KEY"));

// Create using default env var (throws if not set)
Provider provider = ProviderType.OPENAI.create();

// Create with custom base URL (e.g., for proxies)
Provider provider = ProviderType.DEEPSEEK.create(apiKey, "https://my-proxy.com/v1");

// Look up by name
ProviderType type = ProviderType.fromName("groq");
```

**Note:** Cloudflare requires both an account ID and API token, so use `new CloudflareProvider(accountId, token)` directly.

### Supported Provider Types

| Constant | Provider | Env Var |
|---|---|---|
| `ProviderType.OPENAI` | OpenAI | `OPENAI_API_KEY` |
| `ProviderType.ANTHROPIC` | Anthropic | `ANTHROPIC_API_KEY` |
| `ProviderType.GEMINI` | Google Gemini | `GEMINI_API_KEY` |
| `ProviderType.XAI` | xAI Grok | `XAI_API_KEY` |
| `ProviderType.MISTRAL` | Mistral | `MISTRAL_API_KEY` |
| `ProviderType.DEEPSEEK` | DeepSeek | `DEEPSEEK_API_KEY` |
| `ProviderType.COPILOT` | GitHub Copilot | `GITHUB_TOKEN` |
| `ProviderType.OPENROUTER` | OpenRouter | `OPENROUTER_API_KEY` |
| `ProviderType.MOONSHOT` | Moonshot / Kimi | `MOONSHOT_API_KEY` |
| `ProviderType.PERPLEXITY` | Perplexity | `PERPLEXITY_API_KEY` |
| `ProviderType.NOUS` | Nous Portal | `NOUS_API_KEY` |
| `ProviderType.QWEN` | Alibaba Qwen | `DASHSCOPE_API_KEY` |
| `ProviderType.GROQ` | Groq | `GROQ_API_KEY` |
| `ProviderType.TOGETHER` | Together AI | `TOGETHER_API_KEY` |
| `ProviderType.FIREWORKS` | Fireworks AI | `FIREWORKS_API_KEY` |
| `ProviderType.CEREBRAS` | Cerebras | `CEREBRAS_API_KEY` |
| `ProviderType.SAMBANOVA` | SambaNova | `SAMBANOVA_API_KEY` |
| `ProviderType.ZHIPU` | Zhipu AI | `ZHIPU_API_KEY` |
| `ProviderType.MINIMAX` | MiniMax | `MINIMAX_API_KEY` |
| `ProviderType.HUGGINGFACE` | Hugging Face | `HF_TOKEN` |
| `ProviderType.NVIDIA` | NVIDIA NIM | `NVIDIA_API_KEY` |
| `ProviderType.DEEPINFRA` | DeepInfra | `DEEPINFRA_API_KEY` |
| `ProviderType.CLOUDFLARE` | Cloudflare Workers AI | *(use constructor)* |
| `ProviderType.OLLAMA` | Ollama | *(none)* |
| `ProviderType.LMSTUDIO` | LM Studio | *(none)* |
| `ProviderType.LLAMACPP` | llama.cpp | *(none)* |

### Streaming

All providers support streaming. Tokens arrive via a callback as the model generates them:

```java
Provider provider = new OpenAIProvider(System.getenv("OPENAI_API_KEY"));

ChatRequest request = new ChatRequest(
    "gpt-4o",
    List.of(new ChatMessage("user", "Write a haiku about Rust.")),
    256
);

System.out.print("Assistant: ");
provider.streamChat(request, chunk -> {
    String content = chunk.getContent();
    if (content != null) System.out.print(content);
});
System.out.println();
provider.close();
```

### Local Providers

Ollama, LM Studio, and llama.cpp run locally and need no API key:

```java
// Ollama (default: http://localhost:11434)
Provider ollama = new OllamaProvider();

// LM Studio (default: http://localhost:1234/v1)
Provider lmstudio = new LMStudioProvider();

// llama.cpp (default: http://localhost:8080/v1)
Provider llamacpp = new LlamaCppProvider();

// Custom address
Provider ollama = new OllamaProvider("http://192.168.1.100:11434");
```

---

## DeepSeek-Specific Features

The classes `DeepSeekService`, `DeepSeekAPIClient`, and `DeepSeekModels` provide a higher-level, stateful API specifically for DeepSeek. These remain fully supported alongside the new `Provider` interface.

### Active Models

The library ships with constants for the current DeepSeek V4 models:

| Constant | Value | Description |
|---|---|---|
| `DeepSeekModels.DEEPSEEK_V4_FLASH` | `"deepseek-v4-flash"` | Fast and cost-effective — replaces all previous models. 1M context. Thinking mode on by default |
| `DeepSeekModels.DEEPSEEK_V4_PRO` | `"deepseek-v4-pro"` | Highest quality reasoning. Thinking mode on by default |
| `DeepSeekModels.DEEPSEEK_V4_FLASH_VISION_EXP` | `"deepseek-v4-flash-vision-exp"` | Experimental — same as flash plus image input |

You can also query the set at runtime:

```java
DeepSeekModels.getActiveModels().forEach(System.out::println);
boolean vision = DeepSeekModels.supportsVision(DeepSeekModels.DEEPSEEK_V4_FLASH_VISION_EXP); // true
```

### Deprecated Model Resolution

All older DeepSeek model identifiers (`"deepseek-chat"`, `"deepseek-reasoner"`, `"deepseek-coder"`, `"deepseek-v2"`, etc.) are **automatically resolved** to `deepseek-v4-flash` at runtime. The library logs a warning when a deprecated model is used, so you can migrate your code at your own pace.

### Stateful Chat (with History)

Each call to `sendChatRequest` appends both the user message and the assistant reply to an in-memory history. The full history is sent with every subsequent request, giving the model context about previous turns.

```java
DeepSeekService service = new DeepSeekService(System.getenv("DEEPSEEK_API_KEY"), "https://api.deepseek.com", 2048);
String model = DeepSeekModels.DEEPSEEK_V4_PRO;

// Turn 1
String reply1 = service.sendChatRequest("Tell me about the Great Wall of China.", model).getMessage();
System.out.println("Assistant: " + reply1);

// Turn 2 — the model remembers the topic
String reply2 = service.sendChatRequest("How long did it take to build?", model).getMessage();
System.out.println("Assistant: " + reply2);
```

### Single-Turn Completion

`generateCompletion` sends a one-off request and does **not** modify the chat history:

```java
DeepSeekModels.ChatResponse response =
    service.generateCompletion("Summarise the Turing test in one sentence.", DeepSeekModels.DEEPSEEK_V4_FLASH);
System.out.println(response.getMessage());
```

### Streaming Responses (DeepSeek)

Use `streamChatRequest` to receive tokens as they are generated. Conversation history is updated after streaming completes:

```java
System.out.print("Assistant: ");
service.streamChatRequest(
    "Explain quantum entanglement simply.",
    DeepSeekModels.DEEPSEEK_V4_FLASH,
    token -> System.out.print(token)
);
System.out.println();
```

### Vision (Image Input)

`deepseek-v4-flash-vision-exp` accepts images alongside text:

```java
service.sendVisionRequest(
    "What is in this image?",
    DeepSeekModels.DEEPSEEK_V4_FLASH_VISION_EXP,
    2048,
    List.of("data:image/jpeg;base64,<BASE64_DATA>")
);
```

### Thinking Mode (Chain-of-Thought Reasoning)

Both `deepseek-v4-flash` and `deepseek-v4-pro` support thinking mode. When active, the API returns `reasoning_content` alongside `content`:

```java
DeepSeekModels.ChatResponse response = service.sendChatRequest(
    "What is 9.11 vs 9.8?", DeepSeekModels.DEEPSEEK_V4_PRO);
String reasoning = response.getChoices().get(0).getMessage().getReasoningContent();
String answer = response.getMessage();
```

### Listing Available Models

```java
service.getModels().forEach(model ->
    System.out.println(model.getId() + " (owned by: " + model.getOwnedBy() + ")")
);
```

### Clearing Chat History

```java
service.clearChatHistory();
```

---

## API Key Security

**Never hardcode API keys in your source code.** They will end up in version control and leak. Use one of the patterns below instead.

### Environment variable (recommended)

```bash
export GROQ_API_KEY="gsk_..."
```

```java
Provider provider = new GroqProvider(System.getenv("GROQ_API_KEY"));
```

### `.env` file + a loader library

Keep a `.env` file locally (and add it to `.gitignore`):

```
GROQ_API_KEY=gsk_...
```

Load it with a library such as [dotenv-java](https://github.com/cdimascio/dotenv-java):

```java
Dotenv dotenv = Dotenv.load();
Provider provider = new GroqProvider(dotenv.get("GROQ_API_KEY"));
```

### Java system property

```bash
java -Dgroq.api.key="gsk_..." -jar your-app.jar
```

```java
Provider provider = new GroqProvider(System.getProperty("groq.api.key"));
```

---

## API Reference

### `Provider` Interface

| Method | Description |
|---|---|
| `chat(ChatRequest)` | Send a blocking chat completion request |
| `streamChat(ChatRequest, Consumer<ChatStreamChunk>)` | Send a streaming chat request |
| `listModels()` | List available models from this provider |
| `getName()` | Returns the provider name (e.g., `"groq"`, `"openai"`) |
| `close()` | Releases HTTP connection pool |

### `ProviderType` Enum

| Method | Description |
|---|---|
| `create(String apiKey)` | Create provider with explicit API key |
| `create()` | Create provider using default env var |
| `create(String apiKey, String baseUrl)` | Create provider with custom base URL |
| `fromName(String name)` | Look up provider by name string |
| `getProviderName()` | Returns the provider name |
| `getDefaultBaseUrl()` | Returns the default base URL |
| `getEnvVar()` | Returns the env var name (or null for local providers) |

### `DeepSeekService`

| Method | Description |
|---|---|
| `getModels()` | Returns all available models |
| `sendChatRequest(message, model)` | Sends a message; updates history |
| `sendChatRequest(message, model, maxTokens)` | Sends a message with a custom token limit |
| `streamChatRequest(message, model, onToken)` | Streams a response; updates history |
| `generateCompletion(prompt, model)` | Stateless single-turn request |
| `generateCompletion(prompt, model, maxTokens)` | Stateless with custom token limit |
| `sendVisionRequest(prompt, model, maxTokens, imageUrls)` | Single-turn vision request |
| `getChatHistory()` | Returns a snapshot of the current history |
| `clearChatHistory()` | Clears conversation history |
| `setSystemPrompt(String)` | Sets the system message prepended to every request |
| `getSystemPrompt()` | Returns the current system prompt |
| `setDefaultMaxTokens(int)` | Updates the global default token limit |
| `getDefaultMaxTokens()` | Returns the current default token limit |
| `close()` | Releases the underlying HTTP connection pool |

### `ChatResponse`

| Method | Description |
|---|---|
| `getMessage()` | Returns the text content of the first choice |
| `getChoices()` | Returns all response choices |
| `getUsage()` | Returns token usage statistics |
| `getId()` | Returns the response ID |
| `getModel()` | Returns the model that generated the response |

---

## Error Handling

All provider errors throw `ProviderException` (a checked exception). When the API returned an HTTP error response, the status code is available via `getStatusCode()`.

```java
try {
    provider.chat(request);
} catch (ProviderException e) {
    System.err.println("Message:     " + e.getMessage());
    System.err.println("Provider:    " + e.getProviderName());
    System.err.println("Status code: " + e.getStatusCode());
    if (e.isRateLimited()) {
        // rate limited — wait and retry
    }
}
```

### DeepSeek-Specific Errors

The DeepSeek-specific API (`DeepSeekService`) still uses `DeepSeekAPIException` and `DeepSeekHTTPException`:

```java
try {
    service.sendChatRequest("Hello", DeepSeekModels.DEEPSEEK_V4_FLASH);
} catch (DeepSeekAPIException e) {
    System.err.println("Status code: " + e.getStatusCode());
}
```

### HTTP Status Codes

| Status | Meaning | Typical fix |
|---|---|---|
| `400` | Invalid format | Fix the request body per the error hints |
| `401` | Authentication failed | Check the API key |
| `402` | Insufficient balance | Top up the account |
| `422` | Invalid parameters | Fix the request parameters |
| `429` | Rate limit reached | Slow down the request rate |
| `500` | Server error | Retry after a brief wait |
| `503` | Server overloaded | Retry after a brief wait |

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
