package com.github.dedinc.oprouter4j.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dedinc.oprouter4j.core.Config;
import com.github.dedinc.oprouter4j.core.Logger;
import com.github.dedinc.oprouter4j.exception.RateLimitException;
import com.github.dedinc.oprouter4j.model.APIResponse;
import com.github.dedinc.oprouter4j.util.RetryHandler;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Advanced OpenRouter API client with comprehensive error handling and retry logic.
 * Uses production-ready RetryHandler with Resilience4j for intelligent retries.
 */
public class OpenRouterClient implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(OpenRouterClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Config config;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final Semaphore semaphore;
    private final RateLimiter rateLimiter;
    private final RetryHandler retryHandler;

    static {
        objectMapper.findAndRegisterModules();
    }

    public OpenRouterClient() {
        this(null, null);
    }

    public OpenRouterClient(String apiKey, String model) {
        this.config = Config.load();
        this.apiKey = apiKey != null ? apiKey : config.getOpenrouterApiKey();
        this.model = model != null ? model : config.getDefaultModel();
        this.baseUrl = config.getBaseUrl();

        // Create HTTP client with timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(100, 5, TimeUnit.MINUTES))
                .build();

        // Rate limiting
        this.semaphore = new Semaphore(config.getMaxConcurrentRequests());
        this.rateLimiter = new RateLimiter(config.getMaxRequestsPerMinute(), 60);

        // Initialize production-ready retry handler
        this.retryHandler = new RetryHandler(config);

        // Log to file only (no console spam)
        logger.info("Initialized OpenRouter client with model: " + this.model);
    }

    private Map<String, String> getHeaders(Map<String, String> extraHeaders) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");
        headers.put("X-Title", "OpRouter4j Advanced Chat Client");
        headers.put("User-Agent", "OpRouter4j/1.0");

        if (extraHeaders != null) {
            headers.putAll(extraHeaders);
        }

        return headers;
    }

    private APIResponse makeRequest(String method, String endpoint, Map<String, Object> data,
                                    Map<String, String> extraHeaders) throws Exception {
        // Use RetryHandler for production-ready retry logic with exponential backoff
        return retryHandler.execute(() -> {
            try {
                // Acquire semaphore for concurrent request limiting
                semaphore.acquire();

                try {
                    // Rate limiting
                    rateLimiter.acquire();

                    String url = baseUrl + "/" + endpoint.replaceFirst("^/", "");
                    logger.debug("Making " + method + " request to " + url);

                    Request.Builder requestBuilder = new Request.Builder().url(url);

                    // Add headers
                    Map<String, String> headers = getHeaders(extraHeaders);
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        requestBuilder.addHeader(entry.getKey(), entry.getValue());
                    }

                    // Add body for POST requests
                    if ("POST".equals(method) && data != null) {
                        String jsonBody = objectMapper.writeValueAsString(data);
                        requestBuilder.post(RequestBody.create(jsonBody, MediaType.parse("application/json")));
                    }

                    Request request = requestBuilder.build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        int statusCode = response.code();
                        String responseBody = response.body() != null ? response.body().string() : "";

                        Map<String, String> responseHeaders = new HashMap<>();
                        response.headers().names().forEach(name ->
                                responseHeaders.put(name, response.header(name)));

                        // Handle rate limiting
                        if (statusCode == 429) {
                            String retryAfterStr = response.header("Retry-After");
                            int retryAfter = retryAfterStr != null ? Integer.parseInt(retryAfterStr) : 60;
                            throw new RateLimitException("Rate limit exceeded. Retry after " + retryAfter + " seconds", retryAfter);
                        }

                        // Handle client errors (don't retry)
                        if (statusCode >= 400 && statusCode < 500) {
                            String errorMsg = "Client error " + statusCode + ": " + responseBody;
                            logger.error(errorMsg);
                            APIResponse apiResponse = new APIResponse(false, errorMsg);
                            apiResponse.setStatusCode(statusCode);
                            apiResponse.setHeaders(responseHeaders);
                            return apiResponse;
                        }

                        // Handle server errors (will retry)
                        if (statusCode >= 500) {
                            String errorMsg = "Server error " + statusCode + ": " + responseBody;
                            throw new IOException(errorMsg);
                        }

                        // Success response
                        if (statusCode == 200) {
                            try {
                                Map<String, Object> responseData = objectMapper.readValue(responseBody,
                                        new TypeReference<Map<String, Object>>() {
                                        });

                                @SuppressWarnings("unchecked")
                                Map<String, Object> usage = (Map<String, Object>) responseData.get("usage");

                                // Log to file only (no console spam)
                                if (usage != null) {
                                    Object totalTokens = usage.get("total_tokens");
                                    logger.debug("Request successful. Tokens used: " + totalTokens);
                                }

                                APIResponse apiResponse = new APIResponse(true, responseData);
                                apiResponse.setStatusCode(statusCode);
                                apiResponse.setHeaders(responseHeaders);
                                apiResponse.setUsage(usage);
                                return apiResponse;

                            } catch (Exception e) {
                                String errorMsg = "Invalid JSON response: " + e.getMessage();
                                logger.error(errorMsg);
                                APIResponse apiResponse = new APIResponse(false, errorMsg);
                                apiResponse.setStatusCode(statusCode);
                                return apiResponse;
                            }
                        }

                        // Unexpected status code
                        String errorMsg = "Unexpected status code " + statusCode + ": " + responseBody;
                        logger.warning(errorMsg);
                        APIResponse apiResponse = new APIResponse(false, errorMsg);
                        apiResponse.setStatusCode(statusCode);
                        apiResponse.setHeaders(responseHeaders);
                        return apiResponse;
                    }

                } finally {
                    semaphore.release();
                }

            } catch (RateLimitException | IOException e) {
                // RetryHandler will handle these exceptions with exponential backoff
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request interrupted", e);
            } catch (Exception e) {
                throw new RuntimeException("Request failed", e);
            }
        });
    }

    /**
     * Send a chat completion request.
     */
    public APIResponse chatCompletion(List<Map<String, String>> messages) throws Exception {
        return chatCompletion(messages, null, 0.7, null, false);
    }

    /**
     * Send a chat completion request with options.
     */
    public APIResponse chatCompletion(List<Map<String, String>> messages, String model,
                                      double temperature, Integer maxTokens, boolean stream) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model != null ? model : this.model);
        payload.put("messages", messages);
        payload.put("temperature", temperature);

        if (maxTokens != null) {
            payload.put("max_tokens", maxTokens);
        }

        if (stream) {
            payload.put("stream", true);
        }

        // Log to file only (no console spam)
        logger.debug("Sending chat completion request with " + messages.size() + " messages");

        return makeRequest("POST", "/chat/completions", payload, null);
    }

    /**
     * Stream chat completion response.
     */
    public void chatCompletionStream(List<Map<String, String>> messages, Consumer<String> onChunk) throws Exception {
        chatCompletionStream(messages, null, 0.7, null, onChunk);
    }

    /**
     * Stream chat completion response with options.
     */
    public void chatCompletionStream(List<Map<String, String>> messages, String model,
                                     double temperature, Integer maxTokens, Consumer<String> onChunk) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model != null ? model : this.model);
        payload.put("messages", messages);
        payload.put("temperature", temperature);
        payload.put("stream", true);

        if (maxTokens != null) {
            payload.put("max_tokens", maxTokens);
        }

        semaphore.acquire();
        try {
            rateLimiter.acquire();

            String url = baseUrl + "/chat/completions";
            String jsonBody = objectMapper.writeValueAsString(payload);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .headers(Headers.of(getHeaders(null)))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorText = response.body() != null ? response.body().string() : "";
                    throw new IOException("Stream request failed: " + response.code() + " - " + errorText);
                }

                BufferedReader reader = new BufferedReader(new StringReader(
                        response.body() != null ? response.body().string() : ""));

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            break;
                        }

                        try {
                            Map<String, Object> chunk = objectMapper.readValue(data,
                                    new TypeReference<Map<String, Object>>() {
                                    });

                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");

                            if (choices != null && !choices.isEmpty()) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");

                                if (delta != null && delta.containsKey("content")) {
                                    String content = (String) delta.get("content");
                                    onChunk.accept(content);
                                }
                            }
                        } catch (Exception e) {
                            // Skip invalid JSON chunks
                        }
                    }
                }
            }
        } finally {
            semaphore.release();
        }
    }

    /**
     * Get available models.
     */
    public APIResponse getModels() throws Exception {
        return makeRequest("GET", "/models", null, null);
    }

    /**
     * Check if the API is accessible.
     */
    public boolean healthCheck() {
        try {
            APIResponse response = getModels();
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return false;
        }
    }

    /**
     * Get retry statistics.
     */
    public RetryHandler.RetryStats getRetryStats() {
        return retryHandler.getStats();
    }

    /**
     * Get the underlying RetryHandler for advanced usage.
     */
    public RetryHandler getRetryHandler() {
        return retryHandler;
    }

    @Override
    public void close() {
        // Log retry statistics before closing
        RetryHandler.RetryStats stats = retryHandler.getStats();
        if (stats.getTotalCalls() > 0) {
            logger.info("Retry statistics: " + stats.toString());
        }

        // Cleanup resources
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    /**
     * Simple rate limiter implementation.
     */
    private static class RateLimiter {
        private final int maxRequests;
        private final long periodMillis;
        private final long[] requestTimes;
        private int index = 0;

        public RateLimiter(int maxRequests, int periodSeconds) {
            this.maxRequests = maxRequests;
            this.periodMillis = periodSeconds * 1000L;
            this.requestTimes = new long[maxRequests];
        }

        public synchronized void acquire() throws InterruptedException {
            long now = System.currentTimeMillis();
            long oldestRequest = requestTimes[index];

            if (oldestRequest > 0) {
                long timeSinceOldest = now - oldestRequest;
                if (timeSinceOldest < periodMillis) {
                    long waitTime = periodMillis - timeSinceOldest;
                    Thread.sleep(waitTime);
                    now = System.currentTimeMillis();
                }
            }

            requestTimes[index] = now;
            index = (index + 1) % maxRequests;
        }
    }
}

