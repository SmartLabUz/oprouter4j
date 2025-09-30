package com.github.dedinc.oprouter4j.model;

import java.util.Map;

/**
 * Structured API response.
 */
public class APIResponse {
    private boolean success;
    private Map<String, Object> data;
    private String error;
    private Integer statusCode;
    private Map<String, String> headers;
    private Map<String, Object> usage;

    public APIResponse(boolean success) {
        this.success = success;
    }

    public APIResponse(boolean success, Map<String, Object> data) {
        this.success = success;
        this.data = data;
    }

    public APIResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, Object> getUsage() {
        return usage;
    }

    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
}

