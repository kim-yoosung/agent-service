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
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DispatcherServletAdvice {

    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();


    @Advice.OnMethodEnter
    public static void onEnter(@Advice.AllArguments Object[] args) {

        DynamicLogFileGenerator.initLogger();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.AllArguments Object[] args, @Advice.Thrown Throwable t) {
        if (args.length >= 2 &&
                args[0] instanceof HttpServletRequest request &&
                args[1] instanceof HttpServletResponse response) {

            System.out.println("agent 111");
            System.out.println("agent 222");
            try {
                // 요청 바디는 여기서 복사용으로 래핑해서 읽음 (Spring이 다 읽은 뒤)

                CustomRequestWrapper requestWrapper = new CustomRequestWrapper(request);
                CustomResponseWrapper responseWrapper = new CustomResponseWrapper(response);

                System.out.println("agent 333");

                WireMockReqDTO reqDTO = getWireMockReqDTO(requestWrapper);
                WireMockResDTO resDTO = new WireMockResDTO();
                System.out.println("agent 444");
                WiremockDTO dto = new WiremockDTO();
                dto.setRequest(reqDTO);
                dto.setResponse(resDTO);
                captureResponse(responseWrapper, dto);
                logWiremockDTO(dto);

            } catch (Exception e) {
                System.err.println("[Agent] 로깅 중 예외 발생: " + e.getMessage());
            } finally {
                wiremockHolder.remove();
                responseWrapperHolder.remove();
                DynamicLogFileGenerator.finishLogger();
            }
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

        DynamicLogFileGenerator.log(">>> 로깅 시작");

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
        DynamicLogFileGenerator.log(" WiremockDTO 로그:\n" + jsonString);
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
