package com.example.tracing.outgingtracing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClientHttpResponseWrapper {

    private final Object originalResponse;

    public ClientHttpResponseWrapper(Object response) {
        this.originalResponse = response;
    }

    public byte[] getBodyBytes() {
        try {
            Object body = originalResponse.getClass().getMethod("getBody").invoke(originalResponse);

            if (body instanceof InputStream) {
                InputStream bodyStream = (InputStream) body;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                byte[] data = new byte[1024];
                int nRead;
                while ((nRead = bodyStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                return buffer.toByteArray();

            } else if (body instanceof String) {
                // 이미 가공된 문자열이면 그대로 처리
                return ((String) body).getBytes(StandardCharsets.UTF_8);
            } else {
                System.err.println("[Agent] getBody() 리턴 타입 예상 불가: " + body.getClass().getName());
            }

        } catch (Exception e) {
            System.err.println("[Agent] 응답 body 추출 실패: " + e.getMessage());
        }

        return new byte[0];
    }

    public int getStatusCode() {
        try {
            Object statusEnum = originalResponse.getClass().getMethod("getStatusCode").invoke(originalResponse);
            return (int) statusEnum.getClass().getMethod("value").invoke(statusEnum);
        } catch (Exception e) {
            System.err.println("[Agent] 상태코드 추출 실패: " + e.getMessage());
            return 500;
        }
    }

    public Map<String, List<String>> getHeaders() {
        try {
            Object headersObj = originalResponse.getClass().getMethod("getHeaders").invoke(originalResponse);
            if (headersObj instanceof Map) {
                // 타입 안전하게 가져오기
                return (Map<String, List<String>>) headersObj;
            }
        } catch (Exception e) {
            System.err.println("[Agent] 헤더 추출 실패: " + e.getMessage());
        }
        return Collections.emptyMap();
    }
}
