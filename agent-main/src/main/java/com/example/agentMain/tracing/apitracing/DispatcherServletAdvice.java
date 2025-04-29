package com.example.agentMain.tracing.apitracing;

import com.example.logging.DynamicLogFileGenerator;
import com.example.agentMain.tracing.dto.WireMockReqDTO;
import com.example.agentMain.tracing.dto.WireMockResDTO;
import com.example.agentMain.tracing.dto.WiremockDTO;
import net.bytebuddy.asm.Advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DispatcherServletAdvice {

    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomRequestWrapper> requestWrapperHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();

    private static void cleanupThreadLocals() {
        wiremockHolder.remove();
        requestWrapperHolder.remove();
        responseWrapperHolder.remove();
    }

    private static void logRequestDetails(HttpServletRequest request) {
        try {
            if (request != null) {
                System.out.println("[Agent Debug] Request details:");
                System.out.println("  - Class: " + request.getClass().getName());
                System.out.println("  - URI: " + request.getRequestURI());
                System.out.println("  - Method: " + request.getMethod());
                System.out.println("  - Content Type: " + request.getContentType());
                System.out.println("  - Character Encoding: " + request.getCharacterEncoding());
            } else {
                System.out.println("[Agent Warning] Request object is null");
            }
        } catch (Exception e) {
            System.err.println("[Agent Error] Failed to log request details: " + e.getMessage());
        }
    }

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(value = 0, readOnly = false) HttpServletRequest request,
                              @Advice.Argument(value = 1, readOnly = false) HttpServletResponse response) {
        try {
            // ThreadLocal 변수들 초기화
            cleanupThreadLocals();
            
            // 요청 상세 정보 로깅
            System.out.println("[Agent] Starting request processing...");
            logRequestDetails(request);

            DynamicLogFileGenerator.initLogger();
            System.out.println("[Agent] Logger initialized successfully");

            // Request Wrapper 생성 및 로깅
            try {
                CustomRequestWrapper wrappedRequest = new CustomRequestWrapper(request);
                System.out.println("[Agent] Request wrapper created successfully");
                request = wrappedRequest;
                requestWrapperHolder.set(wrappedRequest);
            } catch (Exception e) {
                System.err.println("[Agent Error] Failed to create request wrapper:");
                System.err.println("  - Error type: " + e.getClass().getName());
                System.err.println("  - Error message: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Response Wrapper 생성 및 로깅
            try {
                CustomResponseWrapper wrappedResponse = new CustomResponseWrapper(response);
                System.out.println("[Agent] Response wrapper created successfully");
                response = wrappedResponse;
                responseWrapperHolder.set(wrappedResponse);
            } catch (Exception e) {
                System.err.println("[Agent Error] Failed to create response wrapper:");
                System.err.println("  - Error type: " + e.getClass().getName());
                System.err.println("  - Error message: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

        } catch (Exception e) {
            System.err.println("[Agent Error] OnEnter error occurred:");
            System.err.println("  - Error type: " + e.getClass().getName());
            System.err.println("  - Error message: " + e.getMessage());
            System.err.println("  - Stack trace:");
            e.printStackTrace();
            
            // 에러 발생 시에도 ThreadLocal 정리
            cleanupThreadLocals();
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Thrown Throwable t) {
        try {
            System.out.println("[Agent] Starting exit processing...");
            
            CustomRequestWrapper requestWrapper = requestWrapperHolder.get();
            CustomResponseWrapper responseWrapper = responseWrapperHolder.get();

            if (requestWrapper == null || responseWrapper == null) {
                System.out.println("[Agent] Skipping processing - wrappers are null");
                return;
            }

            try {
                WireMockReqDTO reqDTO = IncomingReqResFilter.getWireMockReqDTO(requestWrapper);
                System.out.println("[Agent] Request DTO created successfully");
                
                WireMockResDTO resDTO = new WireMockResDTO();
                WiremockDTO dto = new WiremockDTO();
                dto.setRequest(reqDTO);
                dto.setResponse(resDTO);

                IncomingReqResFilter.captureResponse(responseWrapper, dto);
                System.out.println("[Agent] Response captured successfully");
                
                IncomingReqResFilter.logWiremockDTO(dto);
                System.out.println("[Agent] DTO logged successfully");

            } catch (Exception e) {
                System.err.println("[Agent Error] Failed to process response:");
                System.err.println("  - Error type: " + e.getClass().getName());
                System.err.println("  - Error message: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("[Agent Error] OnExit error occurred:");
            System.err.println("  - Error type: " + e.getClass().getName());
            System.err.println("  - Error message: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // ThreadLocal 변수들 정리
                cleanupThreadLocals();
                DynamicLogFileGenerator.finishLogger();
                System.out.println("[Agent] Cleanup completed successfully");
            } catch (Exception e) {
                System.err.println("[Agent Error] Cleanup failed: " + e.getMessage());
            }
        }
    }
}
