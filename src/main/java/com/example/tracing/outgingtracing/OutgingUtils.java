package com.example.tracing.outgingtracing;

import com.example.tracing.dto.WireMockReqDTO;
import com.example.tracing.dto.WireMockResDTO;
import com.example.tracing.dto.WiremockDTO;
import com.example.tracing.logging.DynamicLogFileGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OutgingUtils {

    private static final String SELF_SERVICE_NAME = "nucm";
    private static final String[] EXCLUDE_URI_KEYWORDS = { "multiGet" };
    private static final List<String> INACTIVE_SERVER_LIST = Arrays.asList(
            "http://nucube.tpusv-aws.lguplus.co.kr",
            "http://nucube.tprsv-paas.lguplus.co.kr"
    );

    public static boolean shouldSkipRequest(String method, String uri, String serviceName) {
        if (!"GET".equalsIgnoreCase(method) && containsExcludedKeyword(uri)) {
            System.out.println("[Agent] ignore case. method: " + method);
            DynamicLogFileGenerator.log("Outging ReqResInterceptor: " + "[IGNORE] This request may cause data to be written. Skip this testcase.");
            return true;
        }

        if (SELF_SERVICE_NAME.equalsIgnoreCase(serviceName)) {
            System.out.println("[Agent] ignore. 자기 자신 호출");
            DynamicLogFileGenerator.log("Outging ReqResInterceptor: " + "[IGNORE] This request calls its own service.");
            return true;
        }
        return false;
    }

    public static boolean containsExcludedKeyword(String uri) {
        for (String keyword : EXCLUDE_URI_KEYWORDS) {
            if (uri.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static String filterInactiveUrl(String uri) {
        for (String server : INACTIVE_SERVER_LIST) {
            uri = uri.replace(server, "");
        }
        return uri;
    }

    public static String extractServiceName(String uri) {
        try {
            String[] parts = uri.split("/");
            return parts.length > 1 ? parts[1] : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static void handleWiremockLogging(Object[] args,
                                             ClientHttpResponseWrapper response) {
        try {
            WiremockDTO wiremockDTO = buildWiremockDTO(args, response);
            saveAsJson(wiremockDTO);

        } catch (Exception e) {
            System.err.println("[Agent] Wiremock 저장 실패: " + e.getMessage());
        }
    }

    public static WiremockDTO buildWiremockDTO(Object[] args, ClientHttpResponseWrapper wrappedResponse) throws Exception {

        WiremockDTO wiremockDTO = new WiremockDTO();

        Object uri = args[0];
        Object httpMethod = args[1];

        WireMockReqDTO reqDTO = new WireMockReqDTO();
        reqDTO.setUrl(uri.toString());
        reqDTO.setUrlPattern(uri.toString());
        reqDTO.setMethod(httpMethod.toString());

        WireMockResDTO resDTO = new WireMockResDTO();
        resDTO.setStatus(wrappedResponse.getStatusCode());
        resDTO.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        resDTO.setBody(new String(wrappedResponse.getBodyBytes(), StandardCharsets.UTF_8));

        wiremockDTO.setRequest(reqDTO);
        wiremockDTO.setResponse(resDTO);
        return wiremockDTO;
    }

    public static void saveAsJson(WiremockDTO wiremockDTO) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        JsonNode rootNode = mapper.valueToTree(wiremockDTO);
        modifyJsonNode(wiremockDTO, rootNode);

        String jsonString = mapper.writeValueAsString(wiremockDTO);
        String jsonPath = createJsonFile(jsonString);
        DynamicLogFileGenerator.log("OutgingReqResInterceptor: " + jsonPath);
    }

    public static void modifyJsonNode(WiremockDTO wiremockDTO, JsonNode rootNode) {
        if ("GET".equals(wiremockDTO.getRequest().getMethod())) {
            ((ObjectNode) rootNode.get("request")).remove("bodyPatterns");
        }

        if (wiremockDTO.getRequest().getUrl().contains("?")) {
            ((ObjectNode) rootNode.get("request")).remove("urlPattern");
        } else {
            ((ObjectNode) rootNode.get("request")).remove("url");
        }
    }

    public static String createJsonFile(String jsonString) {
        String filePath = "logs/" + "interceptor-" + System.currentTimeMillis() + ".json";

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(jsonString);
            System.out.println(filePath + " created successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filePath;
    }
}
