package com.example.tracing.apitracing;

import com.example.tracing.dto.WireMockReqDTO;
import com.example.tracing.dto.WireMockResDTO;
import com.example.tracing.dto.WiremockDTO;
import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.implementation.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static com.example.tracing.apitracing.IncomingReqResFilter.*;

public class DispatcherServletAdvice {
    @RuntimeType
    public static Object intercept(@Origin Method method,
                                 @This Object target,
                                 @AllArguments Object[] args,
                                 @SuperCall Callable<?> callable) throws Exception {

        // --- Swagger 요청 필터링 시작 ---
        if (args != null && args.length >= 2 && args[0] instanceof HttpServletRequest) {
            HttpServletRequest originalRequest = (HttpServletRequest) args[0];
            String requestURI = originalRequest.getRequestURI();
            if (requestURI != null && 
                (requestURI.startsWith("/swagger-ui") || 
                 requestURI.startsWith("/v3/api-docs") || 
                 requestURI.startsWith("/swagger-resources") || 
                 requestURI.endsWith("/swagger-config")
                 )) {
                return callable.call();
            }
        }
        DynamicLogFileGenerator.initLogger();
        DynamicLogFileGenerator.log("DispatcherServletAdvice 시작");

        try {
            HttpServletRequest request = (HttpServletRequest) args[0];
            HttpServletResponse response = (HttpServletResponse) args[1];

            // 요청 래퍼 생성 (로깅용)
            CustomRequestWrapper wrappedRequest = new CustomRequestWrapper(request);
            CustomResponseWrapper wrappedResponse = new CustomResponseWrapper(response);

            // 요청 로깅
            logRequest(wrappedRequest);

            // 원래 메서드 호출
            Object result = callable.call();

            // 응답 로깅
            logResponse(wrappedResponse);

            // WireMock DTO 생성 및 로깅
            WireMockReqDTO reqDTO = getWireMockReqDTO(wrappedRequest);
            WireMockResDTO resDTO = new WireMockResDTO();
            WiremockDTO dto = new WiremockDTO();
            dto.setRequest(reqDTO);
            dto.setResponse(resDTO);
            captureResponse(wrappedResponse, dto);
            logWiremockDTO(dto);

            return result;

        } catch (Exception e) {
            DynamicLogFileGenerator.log("Intercept 오류: " + e.getMessage());
            System.err.println("[Agent] Intercept 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            DynamicLogFileGenerator.finishLogger();
        }
    }

    private static void logRequest(CustomRequestWrapper request) {
        try {
            System.out.println("Request: " + request.getMethod() + " " + request.getRequestURI() + " Body: " + request.getBodyAsString());
        } catch (Exception e) {
            System.err.println("Error logging request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void logResponse(CustomResponseWrapper response) {
        try {
            System.out.println("Response: Status: " + response.getStatus() + " Body: " + new String(response.getBody(), "UTF-8"));
        } catch (Exception e) {
            System.err.println("Error logging response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
