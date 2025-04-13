package com.example.tracing.outgingtracing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHttpResponseWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ClientHttpResponseWrapper.class);
    private final Object originalResponse;
    private final byte[] cachedBody;

    public ClientHttpResponseWrapper(Object response, byte[] cachedBody) {
        this.originalResponse = response;
        this.cachedBody = cachedBody;
    }

    public byte[] getBodyBytes() {
        return cachedBody;
    }

    public int getStatusCode() {
        try {
            Object statusEnum = originalResponse.getClass().getMethod("getStatusCode").invoke(originalResponse);
            return (int) statusEnum.getClass().getMethod("value").invoke(statusEnum);
        } catch (Exception e) {
            logger.error("[Agent] Failed to extract status code", e);
            return 500;
        }
    }

    public Map<String, List<String>> getHeaders() {
        try {
            Object headersObj = originalResponse.getClass().getMethod("getHeaders").invoke(originalResponse);
            if (headersObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> headers = (Map<String, List<String>>) headersObj;
                return headers;
            }
        } catch (Exception e) {
            logger.error("[Agent] Failed to extract headers", e);
        }
        return Collections.emptyMap();
    }
}
