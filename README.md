# 🤖 OpRouter4j - OpenRouter SDK Client for Java

[![JitPack](https://jitpack.io/v/DedInc/oprouter4j.svg)](https://jitpack.io/#DedInc/oprouter4j)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A Java library for chatting with AI models through OpenRouter. Simple to use, reliable, and feature-rich.

## What is OpRouter4j?

OpRouter4j makes it easy to build AI-powered applications by providing:
- **Simple API**: Chat with AI in just a few lines of code
- **Reliable**: Automatic retries with exponential backoff when things go wrong
- **Smart**: Manages conversations and tracks usage
- **Pure Library**: No CLI dependencies, just a clean Java library

Perfect for Java developers who want to integrate AI chat into their projects without dealing with API complexities.

## ✨ Key Features

- **Easy Integration**: Start chatting with AI in just a few lines of code
- **Automatic Retries**: Handles rate limits and network issues automatically with exponential backoff
- **Conversation Memory**: Save and resume conversations (file or memory storage)
- **Multiple Models**: Works with any OpenRouter-supported AI model
- **Token Tracking**: Monitor usage and costs
- **Streaming Responses**: Get responses as they're generated
- **Rate Limiting**: Built-in rate limiting and concurrent request control
- **Lightweight**: Minimal dependencies, focused on core functionality

## 🚀 Quick Start

### Prerequisites

- Java 11 or higher
- Gradle (for building from source)

### Installation

**Option 1: Add as dependency via JitPack (Recommended)**

Add JitPack repository and dependency to your project:

**Gradle (Kotlin DSL)**
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.DedInc:oprouter4j:1.0")
}
```

**Gradle (Groovy)**
```gradle
// settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle
dependencies {
    implementation 'com.github.DedInc:oprouter4j:1.0'
}
```

**Maven**
```xml
<!-- Add JitPack repository -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<!-- Add dependency -->
<dependencies>
    <dependency>
        <groupId>com.github.DedInc</groupId>
        <artifactId>oprouter4j</artifactId>
        <version>1.0</version>
    </dependency>
</dependencies>
```

**Option 2: Build from source**
```bash
git clone https://github.com/DedInc/oprouter4j.git
cd oprouter4j
./gradlew build
```

### Get Your API Key

1. Sign up at [OpenRouter](https://openrouter.ai/)
2. Get your [API key](https://openrouter.ai/settings/keys) from the dashboard
3. Set it as an environment variable:
   ```bash
   export OPENROUTER_API_KEY="your_api_key_here"
   ```

   Or create a `.env` file in your project root:
   ```
   OPENROUTER_API_KEY=your_api_key_here
   ```

### Start Chatting

**Use in Your Code:**
```java
import com.github.dedinc.oprouter4j.client.OpenRouterClient;
import com.github.dedinc.oprouter4j.model.APIResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        try (OpenRouterClient client = new OpenRouterClient()) {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "Hello!");
            messages.add(message);
            
            APIResponse response = client.chatCompletion(messages);
            
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = 
                    (List<Map<String, Object>>) response.getData().get("choices");
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = 
                    (Map<String, Object>) choices.get(0).get("message");
                System.out.println(msg.get("content"));
            }
        }
    }
}
```

## 📚 Usage Examples

### 1. Simple Chat

```java
import com.github.dedinc.oprouter4j.client.OpenRouterClient;
import com.github.dedinc.oprouter4j.model.APIResponse;

import java.util.*;

public class SimpleChat {
    public static void main(String[] args) throws Exception {
        try (OpenRouterClient client = new OpenRouterClient()) {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "Explain Java in one sentence");
            messages.add(message);
            
            APIResponse response = client.chatCompletion(messages);
            
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = 
                    (List<Map<String, Object>>) response.getData().get("choices");
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = 
                    (Map<String, Object>) choices.get(0).get("message");
                System.out.println(msg.get("content"));
            }
        }
    }
}
```

### 2. Conversation with Memory

```java
import com.github.dedinc.oprouter4j.client.OpenRouterClient;
import com.github.dedinc.oprouter4j.model.MessageRole;
import com.github.dedinc.oprouter4j.storage.Conversation;
import com.github.dedinc.oprouter4j.model.APIResponse;

import java.util.*;

public class ConversationExample {
    public static void main(String[] args) throws Exception {
        try (OpenRouterClient client = new OpenRouterClient()) {
            Conversation conversation = new Conversation(null, "My Chat", null);
            
            // First message
            conversation.addMessage(MessageRole.USER, "What is Java?");
            APIResponse response1 = client.chatCompletion(
                conversation.getMessagesForApi());
            
            if (response1.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = 
                    (List<Map<String, Object>>) response1.getData().get("choices");
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = 
                    (Map<String, Object>) choices.get(0).get("message");
                String content = (String) msg.get("content");
                
                conversation.addMessage(MessageRole.ASSISTANT, content);
                System.out.println("Assistant: " + content);
            }
            
            // Follow-up message (conversation context is maintained)
            conversation.addMessage(MessageRole.USER, "What are its main features?");
            APIResponse response2 = client.chatCompletion(
                conversation.getMessagesForApi());
            
            if (response2.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = 
                    (List<Map<String, Object>>) response2.getData().get("choices");
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = 
                    (Map<String, Object>) choices.get(0).get("message");
                String content = (String) msg.get("content");
                
                conversation.addMessage(MessageRole.ASSISTANT, content);
                System.out.println("Assistant: " + content);
            }
            
            // Save conversation
            conversation.save();
        }
    }
}
```

### 3. Streaming Responses

```java
import com.github.dedinc.oprouter4j.client.OpenRouterClient;

import java.util.*;

public class StreamingExample {
    public static void main(String[] args) throws Exception {
        try (OpenRouterClient client = new OpenRouterClient()) {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "Write a short poem about Java");
            messages.add(message);
            
            System.out.print("Assistant: ");
            client.chatCompletionStream(messages, chunk -> {
                System.out.print(chunk);
            });
            System.out.println();
        }
    }
}
```

### 4. Different Models

```java
import com.github.dedinc.oprouter4j.client.OpenRouterClient;
import com.github.dedinc.oprouter4j.model.APIResponse;

import java.util.*;

public class ModelExample {
    public static void main(String[] args) throws Exception {
        // Use a specific model
        try (OpenRouterClient client = new OpenRouterClient(null, "anthropic/claude-3-haiku")) {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "Hello from Claude!");
            messages.add(message);
            
            APIResponse response = client.chatCompletion(messages);
            
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = 
                    (List<Map<String, Object>>) response.getData().get("choices");
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = 
                    (Map<String, Object>) choices.get(0).get("message");
                System.out.println(msg.get("content"));
            }
        }
    }
}
```

## ⚙️ Configuration

OpRouter4j can be configured via environment variables or a `.env` file:

```bash
# API Configuration
OPENROUTER_API_KEY=your_api_key_here
DEFAULT_MODEL=x-ai/grok-4-fast:free
BASE_URL=https://openrouter.ai/api/v1

# Rate Limiting
MAX_REQUESTS_PER_MINUTE=60
MAX_CONCURRENT_REQUESTS=5

# Retry Configuration
MAX_RETRIES=5
BASE_DELAY=1.0
MAX_DELAY=60.0
BACKOFF_MULTIPLIER=2.0

# Logging
LOG_LEVEL=INFO
LOG_FILE=oprouter.log
ENABLE_LOGGING=true

# Conversation Management
CONVERSATION_HISTORY_LIMIT=100
AUTO_SAVE_CONVERSATIONS=true
CONVERSATIONS_DIR=conversations
STORAGE_TYPE=file  # 'file' or 'memory'
```

## 📖 API Documentation

### OpenRouterClient

Main client for interacting with the OpenRouter API.

**Constructor:**
```java
OpenRouterClient()  // Uses default configuration
OpenRouterClient(String apiKey, String model)  // Custom API key and model
```

**Methods:**
- `APIResponse chatCompletion(List<Map<String, String>> messages)` - Send a chat completion request
- `APIResponse chatCompletion(List<Map<String, String>> messages, String model, double temperature, Integer maxTokens, boolean stream)` - Send with options
- `void chatCompletionStream(List<Map<String, String>> messages, Consumer<String> onChunk)` - Stream responses
- `APIResponse getModels()` - Get available models
- `boolean healthCheck()` - Check API connectivity

### Conversation

Manages a single conversation with message history.

**Constructor:**
```java
Conversation(String id, String title, String model)
```

**Methods:**
- `void addMessage(MessageRole role, String content)` - Add a message
- `List<Map<String, String>> getMessagesForApi()` - Get messages in API format
- `List<Map<String, String>> getContextWindow(int maxTokens)` - Get messages within token limit
- `boolean save()` - Save conversation
- `static Conversation load(String conversationId)` - Load conversation
- `String exportToText()` - Export to text format

### ConversationManager

Manages multiple conversations.

**Methods:**
- `Conversation createConversation()` - Create new conversation
- `Conversation loadConversation(String id)` - Load conversation
- `List<ConversationMetadata> listConversations()` - List all conversations
- `boolean deleteConversation(String id)` - Delete conversation

## 🔧 Building from Source

```bash
# Clone the repository
git clone https://github.com/DedInc/oprouter4j.git
cd oprouter4j

# Build the project
./gradlew build

# Run tests
./gradlew test

# Create JAR
./gradlew jar
```

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 🙏 Acknowledgments

- Based on the Python [oprouter](https://github.com/DedInc/oprouter) library
- Powered by [OpenRouter](https://openrouter.ai/)

