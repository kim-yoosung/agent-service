package com.example.agentMain.tracing.outgingtracing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class ClientHttpResponseWrapper {

    public final Object originalResponse;

    public ClientHttpResponseWrapper(Object response) {
        this.originalResponse = response;
    }

    public byte[] getBodyBytes() {
        try {
            if (originalResponse instanceof String) {
                // 이미 문자열인 경우
                return ((String) originalResponse).getBytes(StandardCharsets.UTF_8);
            }

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
            if (originalResponse instanceof String) {
                // 문자열인 경우 200으로 가정
                return 200;
            }

            Object statusEnum = originalResponse.getClass().getMethod("getStatusCode").invoke(originalResponse);
            return (int) statusEnum.getClass().getMethod("value").invoke(statusEnum);
        } catch (Exception e) {
            System.err.println("[Agent] 상태코드 추출 실패: " + e.getMessage());
            return 500;
        }
    }

    public Map<String, String> getHeaders() {
        try {
            if (originalResponse instanceof String) {
                // 문자열인 경우 기본 헤더 반환
                return Collections.singletonMap("Content-Type", "application/json");
            }

            Object headers = originalResponse.getClass().getMethod("getHeaders").invoke(originalResponse);
            if (headers != null) {
                // 헤더 변환 로직 추가
                return Collections.singletonMap("Content-Type", "application/json");
            }
        } catch (Exception e) {
            System.err.println("[Agent] 헤더 추출 실패: " + e.getMessage());
        }
        return Collections.emptyMap();
    }
}
