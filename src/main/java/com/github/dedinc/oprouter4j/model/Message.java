package com.github.dedinc.oprouter4j.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a chat message.
 */
public class Message {
    @JsonProperty("role")
    private MessageRole role;

    @JsonProperty("content")
    private String content;

    @JsonProperty("timestamp")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    @JsonProperty("tokens")
    private Integer tokens;

    @JsonProperty("cost")
    private Double cost;

    public Message() {
    }

    public Message(MessageRole role, String content, LocalDateTime timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Message(MessageRole role, String content, LocalDateTime timestamp, Integer tokens, Double cost) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.tokens = tokens;
        this.cost = cost;
    }

    /**
     * Convert to API format (role and content only).
     */
    public Map<String, String> toApiFormat() {
        Map<String, String> map = new HashMap<>();
        map.put("role", role.getValue());
        map.put("content", content);
        return map;
    }

    // Getters and Setters
    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getTokens() {
        return tokens;
    }

    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }
}

