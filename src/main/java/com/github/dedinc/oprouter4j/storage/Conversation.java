package com.github.dedinc.oprouter4j.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dedinc.oprouter4j.core.Config;
import com.github.dedinc.oprouter4j.core.Logger;
import com.github.dedinc.oprouter4j.model.ConversationMetadata;
import com.github.dedinc.oprouter4j.model.Message;
import com.github.dedinc.oprouter4j.model.MessageRole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Conversation management.
 */
public class Conversation {
    private static final Logger logger = Logger.getLogger(Conversation.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.findAndRegisterModules();
    }

    private final Config config;
    private final String id;
    private String title;
    private String model;
    private final List<Message> messages;
    private ConversationMetadata metadata;
    private final Path conversationsDir;

    public Conversation(String conversationId, String title, String model) {
        this.config = Config.load();
        this.id = conversationId != null ? conversationId : UUID.randomUUID().toString();
        this.title = title != null ? title : "Conversation " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.model = model != null ? model : config.getDefaultModel();
        this.messages = new ArrayList<>();
        this.metadata = new ConversationMetadata(this.id, this.title,
                LocalDateTime.now(), LocalDateTime.now(), this.model);
        this.conversationsDir = Paths.get(config.getConversationsDir());

        try {
            Files.createDirectories(conversationsDir);
        } catch (IOException e) {
            logger.error("Failed to create conversations directory", e);
        }

        logger.info("Initialized conversation: " + this.id);
    }

    /**
     * Add a message to the conversation.
     */
    public void addMessage(MessageRole role, String content, Integer tokens, Double cost) {
        Message message = new Message(role, content, LocalDateTime.now(), tokens, cost);
        messages.add(message);
        updateMetadata(tokens, cost);

        // Auto-save if enabled
        if (config.isAutoSaveConversations()) {
            save();
        }

        logger.debug("Added " + role + " message with " + content.length() + " characters");
    }

    /**
     * Add a message to the conversation.
     */
    public void addMessage(MessageRole role, String content) {
        addMessage(role, content, null, null);
    }

    private void updateMetadata(Integer tokens, Double cost) {
        metadata.setUpdatedAt(LocalDateTime.now());
        metadata.setMessageCount(messages.size());

        if (tokens != null) {
            metadata.setTotalTokens(metadata.getTotalTokens() + tokens);
        }
        if (cost != null) {
            metadata.setTotalCost(metadata.getTotalCost() + cost);
        }
    }

    /**
     * Get messages in API format.
     */
    public List<Map<String, String>> getMessagesForApi(Integer limit) {
        List<Message> messagesToConvert = messages;
        if (limit != null && limit > 0 && messages.size() > limit) {
            messagesToConvert = messages.subList(messages.size() - limit, messages.size());
        }

        return messagesToConvert.stream()
                .map(Message::toApiFormat)
                .collect(Collectors.toList());
    }

    /**
     * Get messages in API format.
     */
    public List<Map<String, String>> getMessagesForApi() {
        return getMessagesForApi(null);
    }

    /**
     * Get messages that fit within token limit.
     */
    public List<Map<String, String>> getContextWindow(int maxTokens) {
        // Simple approximation: 4 characters per token
        int currentTokens = 0;
        List<Map<String, String>> contextMessages = new ArrayList<>();

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            int messageTokens = message.getContent().length() / 4;

            if (currentTokens + messageTokens > maxTokens) {
                break;
            }

            contextMessages.add(0, message.toApiFormat());
            currentTokens += messageTokens;
        }

        return contextMessages;
    }

    /**
     * Save conversation based on storage type configuration.
     */
    public boolean save() {
        if (config.isMemoryStorage()) {
            // Store in memory (handled by ConversationManager)
            return true;
        } else if (config.isFileStorage()) {
            try {
                Path filePath = conversationsDir.resolve(id + ".json");

                Map<String, Object> data = new HashMap<>();
                data.put("metadata", metadata);
                data.put("messages", messages);

                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(filePath.toFile(), data);

                logger.info("Saved conversation to " + filePath);
                return true;

            } catch (IOException e) {
                logger.error("Failed to save conversation", e);
                return false;
            }
        } else {
            logger.error("Unsupported storage type: " + config.getStorageType());
            return false;
        }
    }

    /**
     * Load conversation from file.
     */
    public static Conversation load(String conversationId) {
        Config config = Config.load();
        Logger logger = Logger.getLogger(Conversation.class);

        try {
            if (config.isMemoryStorage()) {
                logger.warning("Cannot load individual conversations when using memory storage");
                return null;
            }

            Path filePath = Paths.get(config.getConversationsDir()).resolve(conversationId + ".json");

            if (!Files.exists(filePath)) {
                logger.warning("Conversation file not found: " + filePath);
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(filePath.toFile(), Map.class);

            // Create conversation instance
            ConversationMetadata metadata = objectMapper.convertValue(
                    data.get("metadata"), ConversationMetadata.class);

            Conversation conversation = new Conversation(
                    metadata.getId(), metadata.getTitle(), metadata.getModel());
            conversation.metadata = metadata;

            // Load messages
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messagesData = (List<Map<String, Object>>) data.get("messages");

            for (Map<String, Object> msgData : messagesData) {
                Message message = objectMapper.convertValue(msgData, Message.class);
                conversation.messages.add(message);
            }

            logger.info("Loaded conversation: " + conversationId);
            return conversation;

        } catch (IOException e) {
            logger.error("Failed to load conversation " + conversationId, e);
            return null;
        }
    }

    /**
     * Export conversation to text format.
     */
    public String exportToText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Conversation: ").append(title).append("\n");
        sb.append("ID: ").append(id).append("\n");
        sb.append("Model: ").append(model).append("\n");
        sb.append("Created: ").append(metadata.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("Messages: ").append(metadata.getMessageCount()).append("\n");
        sb.append("Total Tokens: ").append(metadata.getTotalTokens()).append("\n");
        sb.append("Total Cost: $").append(String.format("%.4f", metadata.getTotalCost())).append("\n");
        sb.append("=".repeat(50)).append("\n\n");

        for (Message message : messages) {
            String timestamp = message.getTimestamp()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String role = message.getRole().toString().toUpperCase();
            sb.append("[").append(timestamp).append("] ").append(role).append(":\n");
            sb.append(message.getContent()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Clear all messages.
     */
    public void clear() {
        messages.clear();
        updateMetadata(null, null);
        logger.info("Cleared conversation messages");
    }

    /**
     * Set conversation title.
     */
    public void setTitle(String title) {
        this.title = title;
        this.metadata.setTitle(title);
        updateMetadata(null, null);
        logger.info("Updated conversation title to: " + title);
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public ConversationMetadata getMetadata() {
        return metadata;
    }
}

