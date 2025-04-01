package com.example.tracing.apitracing;

import com.example.tracing.logging.DynamicLogFileGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.bytebuddy.asm.Advice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class DispatcherServletAdvice {

    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomRequestWrapper> requestWrapperHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(value = 0, readOnly = false) HttpServletRequest request,
                               @Advice.Argument(value = 1, readOnly = false) HttpServletResponse response) {
        try {
            DynamicLogFileGenerator.initLogger();

            CustomRequestWrapper wrappedRequest = new CustomRequestWrapper(request);
            CustomResponseWrapper wrappedResponse = new CustomResponseWrapper(response);

            request = wrappedRequest;
            response = wrappedResponse;

            requestWrapperHolder.set(wrappedRequest);
            responseWrapperHolder.set(wrappedResponse);

        } catch (Exception e) {
            System.err.println("[Agent] OnEnter 오류: " + e.getMessage());
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Thrown Throwable t) {
        try {
            CustomRequestWrapper requestWrapper = requestWrapperHolder.get();
            CustomResponseWrapper responseWrapper = responseWrapperHolder.get();

            if (requestWrapper == null || responseWrapper == null) return;

            WireMockReqDTO reqDTO = getWireMockReqDTO(requestWrapper);
            WireMockResDTO resDTO = new WireMockResDTO();

            WiremockDTO dto = new WiremockDTO();
            dto.setRequest(reqDTO);
            dto.setResponse(resDTO);

            captureResponse(responseWrapper, dto);
            logWiremockDTO(dto);

        } catch (Exception e) {
            System.err.println("[Agent] OnExit 로깅 오류: " + e.getMessage());
        } finally {
            wiremockHolder.remove();
            requestWrapperHolder.remove();
            responseWrapperHolder.remove();
            DynamicLogFileGenerator.finishLogger();
        }
    }

    public static void captureResponse(CustomResponseWrapper responseWrapper, WiremockDTO dto) throws IOException {
        byte[] responseBody = responseWrapper.toByteArray();
        String responseBodyString = new String(responseBody, "UTF-8");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        dto.getResponse().setBody(responseBodyString);
        dto.getResponse().setStatus(responseWrapper.getStatus());
        dto.getResponse().setHeaders(headers);
    }

    public static void logWiremockDTO(WiremockDTO wiremockDTO) throws IOException {

        System.out.println("[agent] >>> 로깅 시작");

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
        DynamicLogFileGenerator.log(" IncomingReqResFilter:\n" + jsonString);
    }


    public static WireMockReqDTO getWireMockReqDTO(CustomRequestWrapper request) throws IOException {
        WireMockReqDTO reqDTO = new WireMockReqDTO();

        // HTTP 메서드 설정
        reqDTO.setMethod(request.getMethod());

        // URI 및 URI 패턴 설정
        String fullUri = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            fullUri = fullUri + "?" + request.getQueryString();
        }
        reqDTO.setUrl(fullUri);
        reqDTO.setUriPattern(fullUri);
        reqDTO.setBody(buildBodyPatterns(request));

        // 헤더 정보 저장
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

    public static List<Map<String, String>> buildBodyPatterns(CustomRequestWrapper request) throws IOException {
        StringBuilder body = new StringBuilder();

        System.out.println("request !!!!" + request.getReader());
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
