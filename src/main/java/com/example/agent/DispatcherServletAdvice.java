package com.example.agent;

import com.example.tracing.apitracing.*;
import net.bytebuddy.asm.Advice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class DispatcherServletAdvice {

    private static final Logger logger = Logger.getLogger(DispatcherServletAdvice.class.getName());

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.AllArguments Object[] args) {
        if (args.length >= 2 && args[0] instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) args[0];
            HttpServletResponse response = (HttpServletResponse) args[1];

            System.out.println("[Agent] >>> 요청: [" + request.getMethod() + "] " + request.getRequestURI());
            System.out.println("[Agent] >>> 응답 상태: [" + response.getStatus() + "] ");

            try {
                // 요청 바디 저장 (Wrapper 활용)
                CustomRequestWrapper httpRequest = new CustomRequestWrapper(request);

                System.out.println("[agent]" +  httpRequest);
                CustomResponseWrapper httpResponse = new CustomResponseWrapper(response);
                System.out.println("[agent]" +  httpResponse);

                WiremockDTO wiremockDTO = buildWireMockDTO(httpRequest, httpResponse);

                callDispatcherServlet(httpRequest, httpResponse);

            } catch (IOException e) {
                System.err.println("[Agent] 요청 바디 읽기 실패: " + e.getMessage());
            }
        }
    }

    public static void callDispatcherServlet(HttpServletRequest request, HttpServletResponse response) {
        try {
            Object dispatcherServletObj = request.getServletContext().getAttribute("dispatcherServlet");

            if (dispatcherServletObj == null) {
                System.err.println("[Agent] DispatcherServlet을 찾을 수 없습니다!");
                return;
            }

            // Reflection을 사용하여 service() 메서드 실행
            Class<?> dispatcherClass = dispatcherServletObj.getClass();
            java.lang.reflect.Method serviceMethod = dispatcherClass.getMethod("service", HttpServletRequest.class, HttpServletResponse.class);
            serviceMethod.invoke(dispatcherServletObj, request, response);

        } catch (Exception e) {
            System.err.println("[Agent] DispatcherServlet 호출 중 오류 발생: " + e.getMessage());
        }
    }

    public static WiremockDTO buildWireMockDTO(CustomRequestWrapper request, CustomResponseWrapper response) throws IOException {
        WiremockDTO wiremockDTO = new WiremockDTO();
        wiremockDTO.setRequest(getWireMockReqDTO(request));
        wiremockDTO.setResponse(getWireMockResDTO(response));
        System.out.println("[agent - wiremock]" +  wiremockDTO);

        return wiremockDTO;
    }

    public static WireMockReqDTO getWireMockReqDTO(CustomRequestWrapper request) throws IOException {
        WireMockReqDTO reqDTO = new WireMockReqDTO();

        // 1. HTTP 메서드 설정
        reqDTO.setMethod(request.getMethod());

        // 2. URI 및 URI 패턴 설정
        String fullUri = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            fullUri = fullUri + "?" + request.getQueryString();
        }
        reqDTO.setUrl(fullUri);
        reqDTO.setUriPattern(fullUri);  // URI 패턴이 필요한 경우
        reqDTO.setBody(buildBodyPatterns(request));

        // 4. 헤더 정보 저장
        Map<String, String> headerMap = new HashMap<>(Collections.singletonMap("Content-Type", "application/json"));
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headerMap.put(headerName, headerValue);
        }
        reqDTO.setHeaders(headerMap);

        return reqDTO;
    }

    public static WireMockResDTO getWireMockResDTO(CustomResponseWrapper response) throws IOException {
        WireMockResDTO resDTO = new WireMockResDTO();
        resDTO.setBody(new String(response.toByteArray(), StandardCharsets.UTF_8));
        resDTO.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        resDTO.setStatus(response.getStatus());
        return resDTO;
    }

    /**
     * 요청 바디를 JSON 형식으로 변환
     */
    public static List<Map<String, String>> buildBodyPatterns(CustomRequestWrapper request) throws IOException {
        StringBuilder body = new StringBuilder();

        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        Map<String, String> map = new HashMap<>();
        map.put("equalToJson", body.toString());

        return Collections.singletonList(map);
    }


    public static void captureResponse(CustomResponseWrapper responseWrapper, WiremockDTO wiremockDTO) throws IOException {
        byte[] responseBody = responseWrapper.toByteArray();
        WireMockResDTO resDTO = new WireMockResDTO();
        resDTO.setStatus(responseWrapper.getStatus());
        resDTO.setBody(new String(responseBody, "UTF-8"));
        wiremockDTO.setResponse(resDTO);
    }

    public static void logWiremockDTO(WiremockDTO wiremockDTO) {
        System.out.println("[Agent] WiremockDTO: " + wiremockDTO);
    }
}
