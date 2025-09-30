package com.github.dedinc.oprouter4j.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Application configuration for OpRouter4j.
 * Loads configuration from environment variables and .env file.
 */
public class Config {
    // API Configuration
    private String openrouterApiKey;
    private String defaultModel;
    private String baseUrl;

    // Rate Limiting
    private int maxRequestsPerMinute;
    private int maxConcurrentRequests;

    // Retry Configuration
    private int maxRetries;
    private double baseDelay;
    private double maxDelay;
    private double backoffMultiplier;

    // Logging
    private String logLevel;
    private String logFile;
    private boolean enableLogging;

    // UI Configuration
    private boolean useEmojis;

    // Conversation Management
    private int conversationHistoryLimit;
    private boolean autoSaveConversations;
    private String conversationsDir;
    private String storageType;

    private Config() {
        // Private constructor - use builder or load methods
    }

    /**
     * Load configuration from environment variables and .env file.
     */
    public static Config load() {
        Config config = new Config();
        Properties props = new Properties();

        // Try to load .env file
        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            try (InputStream input = new FileInputStream(envPath.toFile())) {
                props.load(input);
            } catch (IOException e) {
                // Ignore - will use defaults
            }
        }

        // API Configuration
        config.openrouterApiKey = getProperty(props, "OPENROUTER_API_KEY", null);
        config.defaultModel = getProperty(props, "DEFAULT_MODEL", "x-ai/grok-4-fast:free");
        config.baseUrl = getProperty(props, "BASE_URL", "https://openrouter.ai/api/v1");

        // Rate Limiting
        config.maxRequestsPerMinute = Integer.parseInt(getProperty(props, "MAX_REQUESTS_PER_MINUTE", "60"));
        config.maxConcurrentRequests = Integer.parseInt(getProperty(props, "MAX_CONCURRENT_REQUESTS", "5"));

        // Retry Configuration
        config.maxRetries = Integer.parseInt(getProperty(props, "MAX_RETRIES", "5"));
        config.baseDelay = Double.parseDouble(getProperty(props, "BASE_DELAY", "1.0"));
        config.maxDelay = Double.parseDouble(getProperty(props, "MAX_DELAY", "60.0"));
        config.backoffMultiplier = Double.parseDouble(getProperty(props, "BACKOFF_MULTIPLIER", "2.0"));

        // Logging
        config.logLevel = getProperty(props, "LOG_LEVEL", "INFO");
        config.logFile = getProperty(props, "LOG_FILE", "oprouter.log");
        config.enableLogging = Boolean.parseBoolean(getProperty(props, "ENABLE_LOGGING", "true"));

        // UI Configuration
        config.useEmojis = Boolean.parseBoolean(getProperty(props, "USE_EMOJIS", "true"));

        // Conversation Management
        config.conversationHistoryLimit = Integer.parseInt(getProperty(props, "CONVERSATION_HISTORY_LIMIT", "100"));
        config.autoSaveConversations = Boolean.parseBoolean(getProperty(props, "AUTO_SAVE_CONVERSATIONS", "true"));
        config.conversationsDir = getProperty(props, "CONVERSATIONS_DIR", "conversations");
        config.storageType = getProperty(props, "STORAGE_TYPE", "file").toLowerCase();

        // Validate storage type
        if (!config.storageType.equals("file") && !config.storageType.equals("memory")) {
            throw new IllegalArgumentException("storage_type must be 'file' or 'memory', got '" + config.storageType + "'");
        }

        return config;
    }

    private static String getProperty(Properties props, String key, String defaultValue) {
        // First check environment variables
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Then check properties file
        String propValue = props.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            // Strip inline comments (everything after #)
            int commentIndex = propValue.indexOf('#');
            if (commentIndex >= 0) {
                propValue = propValue.substring(0, commentIndex);
            }
            // Trim whitespace
            propValue = propValue.trim();
            return propValue;
        }

        // Return default
        return defaultValue;
    }

    public boolean isMemoryStorage() {
        return "memory".equals(storageType);
    }

    public boolean isFileStorage() {
        return "file".equals(storageType);
    }

    // Getters
    public String getOpenrouterApiKey() {
        return openrouterApiKey;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public double getBaseDelay() {
        return baseDelay;
    }

    public double getMaxDelay() {
        return maxDelay;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getLogFile() {
        return logFile;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public boolean isUseEmojis() {
        return useEmojis;
    }

    public int getConversationHistoryLimit() {
        return conversationHistoryLimit;
    }

    public boolean isAutoSaveConversations() {
        return autoSaveConversations;
    }

    public String getConversationsDir() {
        return conversationsDir;
    }

    public String getStorageType() {
        return storageType;
    }

    // Setters for runtime configuration
    public void setOpenrouterApiKey(String openrouterApiKey) {
        this.openrouterApiKey = openrouterApiKey;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    /**
     * Ensure required directories exist.
     */
    public void ensureDirectories() {
        try {
            Path conversationsPath = Paths.get(conversationsDir);
            Files.createDirectories(conversationsPath);

            Path logPath = Paths.get(logFile).getParent();
            if (logPath != null && !logPath.equals(Paths.get("."))) {
                Files.createDirectories(logPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directories", e);
        }
    }
}

