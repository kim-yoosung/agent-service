package com.example.agent;

import com.example.tracing.apitracing.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.bytebuddy.asm.Advice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class DispatcherServletAdvice {

//    public static final Logger logger = Logger.getLogger(DispatcherServletAdvice.class.getName());
    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();


    @Advice.OnMethodEnter
    public static void onEnter(@Advice.AllArguments Object[] args) {
        if (args.length >= 2 && args[0] instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) args[0];
            HttpServletResponse response = (HttpServletResponse) args[1];

//            logger.info("[Agent] >>> 요청: [" + request.getMethod() + "] " + request.getRequestURI());
//            System.out.println("[Agent] >>> 응답 상태: [" + response.getStatus() + "] ");

            try {

                DynamicLogFileGenerator.initLogger();

                // 요청 바디 저장 (Wrapper 활용)
                CustomRequestWrapper httpRequest = new CustomRequestWrapper(request);

                CustomResponseWrapper httpResponse = new CustomResponseWrapper(response);

                WireMockReqDTO reqDTO = getWireMockReqDTO(httpRequest);
                WireMockResDTO resDTO = new WireMockResDTO();

                WiremockDTO wiremockDTO = new WiremockDTO();
                wiremockDTO.setRequest(reqDTO);
                wiremockDTO.setResponse(resDTO);

                wiremockHolder.set(wiremockDTO);
                responseWrapperHolder.set(httpResponse);

            } catch (IOException e) {
                System.err.println("[Agent] 요청 바디 읽기 실패: " + e.getMessage());
            }
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.AllArguments Object[] args, @Advice.Thrown Throwable t) {
        try {
            WiremockDTO dto = wiremockHolder.get();
            CustomResponseWrapper resWrapper = responseWrapperHolder.get();

            if (dto != null && resWrapper != null) {
                captureResponse(resWrapper, dto);
                logWiremockDTO(dto);
            }
        } catch (Exception e) {
            System.err.println("[Agent] 로깅 중 예외 발생: " + e.getMessage());
        } finally {
            wiremockHolder.remove();
            responseWrapperHolder.remove();

            DynamicLogFileGenerator.finishLogger();
        }
    }

    public static void captureResponse(CustomResponseWrapper responseWrapper, WiremockDTO dto) throws IOException {
        byte[] responseBody = responseWrapper.toByteArray();
        String responseBodyString = new String(responseBody, "UTF-8");

        dto.getResponse().setBody(responseBodyString);
        dto.getResponse().setStatus(responseWrapper.getStatus());
        dto.getResponse().setHeaders(Map.of("Content-Type", "application/json")); // 필요한 경우 실제 헤더 추출
    }

    public static void logWiremockDTO(WiremockDTO wiremockDTO) throws IOException {

        DynamicLogFileGenerator.log("[Agent] >>> 로깅 시작");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        JsonNode rootNode = objectMapper.valueToTree(wiremockDTO);

        if ("GET".equalsIgnoreCase(wiremockDTO.getRequest().getMethod())) {
            ((ObjectNode) rootNode.get("request")).remove("bodyPatterns");
        }

        if (wiremockDTO.getRequest().getUrl().contains("?")) {
            ((ObjectNode) rootNode.get("request")).remove("urlPattern");
        } else {
            ((ObjectNode) rootNode.get("request")).remove("url");
        }

        String jsonString = objectMapper.writeValueAsString(rootNode);
        DynamicLogFileGenerator.log("[Agent] WiremockDTO 로그:\n" + jsonString);
    }

    public static WiremockDTO buildWireMockDTO(CustomRequestWrapper request, CustomResponseWrapper response) throws IOException {
        WiremockDTO wiremockDTO = new WiremockDTO();
        wiremockDTO.setRequest(getWireMockReqDTO(request));
        wiremockDTO.setResponse(getWireMockResDTO(response));

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

}
