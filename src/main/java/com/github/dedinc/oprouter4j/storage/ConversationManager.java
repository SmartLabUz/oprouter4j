package com.github.dedinc.oprouter4j.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dedinc.oprouter4j.core.Config;
import com.github.dedinc.oprouter4j.core.Logger;
import com.github.dedinc.oprouter4j.model.ConversationMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Manage multiple conversations.
 */
public class ConversationManager {
    private static final Logger logger = Logger.getLogger(ConversationManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.findAndRegisterModules();
    }

    private final Config config;
    private final Path conversationsDir;
    private Conversation currentConversation;
    private final Map<String, Conversation> memoryConversations;

    public ConversationManager() {
        this.config = Config.load();
        this.conversationsDir = Paths.get(config.getConversationsDir());
        this.memoryConversations = new HashMap<>();

        // Only create directory if using file storage
        if (config.isFileStorage()) {
            try {
                Files.createDirectories(conversationsDir);
            } catch (IOException e) {
                logger.error("Failed to create conversations directory", e);
            }
        }
    }

    /**
     * Create a new conversation.
     */
    public Conversation createConversation(String title, String model) {
        Conversation conversation = new Conversation(null, title, model);
        currentConversation = conversation;

        // Store in memory if using memory storage
        if (config.isMemoryStorage()) {
            memoryConversations.put(conversation.getId(), conversation);
        }

        return conversation;
    }

    /**
     * Create a new conversation with defaults.
     */
    public Conversation createConversation() {
        return createConversation(null, null);
    }

    /**
     * Load and set as current conversation.
     */
    public Conversation loadConversation(String conversationId) {
        if (config.isMemoryStorage()) {
            // Load from memory
            Conversation conversation = memoryConversations.get(conversationId);
            if (conversation != null) {
                currentConversation = conversation;
            }
            return conversation;
        } else if (config.isFileStorage()) {
            // Load from file
            Conversation conversation = Conversation.load(conversationId);
            if (conversation != null) {
                currentConversation = conversation;
            }
            return conversation;
        } else {
            logger.error("Unsupported storage type: " + config.getStorageType());
            return null;
        }
    }

    /**
     * List all saved conversations.
     */
    public List<ConversationMetadata> listConversations() {
        List<ConversationMetadata> conversations = new ArrayList<>();

        if (config.isMemoryStorage()) {
            // List from memory
            for (Conversation conversation : memoryConversations.values()) {
                conversations.add(conversation.getMetadata());
            }
        } else if (config.isFileStorage()) {
            // List from files
            try (Stream<Path> paths = Files.list(conversationsDir)) {
                paths.filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> data = objectMapper.readValue(path.toFile(), Map.class);
                                ConversationMetadata metadata = objectMapper.convertValue(
                                        data.get("metadata"), ConversationMetadata.class);
                                conversations.add(metadata);
                            } catch (IOException e) {
                                logger.warning("Failed to load conversation metadata from " + path + ": " + e.getMessage());
                            }
                        });
            } catch (IOException e) {
                logger.error("Failed to list conversations", e);
            }
        } else {
            logger.error("Unsupported storage type: " + config.getStorageType());
        }

        // Sort by updated_at descending
        conversations.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
        return conversations;
    }

    /**
     * Delete a conversation.
     */
    public boolean deleteConversation(String conversationId) {
        try {
            if (config.isMemoryStorage()) {
                // Delete from memory
                if (memoryConversations.containsKey(conversationId)) {
                    memoryConversations.remove(conversationId);
                    logger.info("Deleted conversation from memory: " + conversationId);

                    // Clear current conversation if it's the deleted one
                    if (currentConversation != null && currentConversation.getId().equals(conversationId)) {
                        currentConversation = null;
                    }

                    return true;
                } else {
                    logger.warning("Conversation not found in memory: " + conversationId);
                    return false;
                }
            } else if (config.isFileStorage()) {
                // Delete from file
                Path filePath = conversationsDir.resolve(conversationId + ".json");
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Deleted conversation: " + conversationId);

                    // Clear current conversation if it's the deleted one
                    if (currentConversation != null && currentConversation.getId().equals(conversationId)) {
                        currentConversation = null;
                    }

                    return true;
                } else {
                    logger.warning("Conversation file not found: " + conversationId);
                    return false;
                }
            } else {
                logger.error("Unsupported storage type: " + config.getStorageType());
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed to delete conversation " + conversationId, e);
            return false;
        }
    }

    /**
     * Get current conversation.
     */
    public Conversation getCurrentConversation() {
        return currentConversation;
    }

    /**
     * Set current conversation.
     */
    public void setCurrentConversation(Conversation conversation) {
        this.currentConversation = conversation;
    }
}

