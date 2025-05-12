package com.example.agentMain.tracing.outgoingtracing;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
                return ((String) originalResponse).getBytes(StandardCharsets.UTF_8);
            }

            Object body = originalResponse.getClass().getMethod("getBody").invoke(originalResponse);

            if (body == null) {
                System.out.println("[Agent] getBody() 결과: null");
                return new byte[0];
            }

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
                // JSON 직렬화 시도
                try {
                    String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
                    return json.getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    System.err.println("[Agent] JSON 변환 실패: " + e.getMessage());
                }
            }

        } catch (NoSuchMethodException e) {
            System.err.println("[Agent] getBody() 메서드 없음: " + e.getMessage());
        } catch (InvocationTargetException | IllegalAccessException e) {
            System.err.println("[Agent] getBody() 호출 실패: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[Agent] InputStream 읽기 실패: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Agent] 알 수 없는 오류 (getBodyBytes): " + e.getMessage());
        }

        return new byte[0];
    }


    public int getStatusCode() {
        try {
            if (originalResponse instanceof String) {
                return 200;
            }
            // getStatusCode()
            Method getStatusCodeMethod = originalResponse.getClass().getMethod("getStatusCode");
            Object statusEnum = getStatusCodeMethod.invoke(originalResponse);

            if (statusEnum == null) {
                System.err.println("[Agent] getStatusCode() 결과가 null입니다.");
                return 200;
            }

            Method valueMethod = statusEnum.getClass().getMethod("value");
            Object code = valueMethod.invoke(statusEnum);

            return (code instanceof Integer) ? (Integer) code : 500;

        } catch (NoSuchMethodException e) {
            System.err.println("[Agent] 상태코드 메서드 없음: " + e.getMessage());
        } catch (InvocationTargetException | IllegalAccessException e) {
            System.err.println("[Agent] 상태코드 추출 실패: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Agent] 알 수 없는 상태코드 오류: " + e.getMessage());
        }

        return 500;
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
