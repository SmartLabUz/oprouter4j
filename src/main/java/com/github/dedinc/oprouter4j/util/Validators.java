package com.github.dedinc.oprouter4j.util;

/**
 * Validation utilities for OpRouter4j.
 */
public class Validators {

    /**
     * Validate API key format.
     */
    public static boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }

        // OpenRouter API keys typically start with "sk-or-v1-"
        if (apiKey.startsWith("sk-or-v1-") && apiKey.length() > 20) {
            return true;
        }

        // Also accept other formats for flexibility
        return apiKey.length() > 10;
    }
}

