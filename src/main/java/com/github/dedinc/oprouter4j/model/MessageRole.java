package com.github.dedinc.oprouter4j.model;

/**
 * Message roles in a conversation.
 */
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String value;

    MessageRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MessageRole fromValue(String value) {
        for (MessageRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown message role: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}

