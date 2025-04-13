package com.example.tracing.apitracing;

import com.example.tracing.dto.WireMockReqDTO;
import com.example.tracing.dto.WireMockResDTO;
import com.example.tracing.dto.WiremockDTO;
import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.implementation.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static com.example.tracing.apitracing.IncomingReqResFilter.*;

public class DispatcherServletAdvice {
    private static final Logger logger = LoggerFactory.getLogger(DispatcherServletAdvice.class);

    @RuntimeType
    public static Object intercept(@Origin Method method,
                                 @This Object target,
                                 @AllArguments Object[] args,
                                 @SuperCall Callable<?> callable) throws Exception {
        try {
            DynamicLogFileGenerator.initLogger();
            logger.info("[Agent] DispatcherServletAdvice start");

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
            logger.error("[Agent] Intercept 오류", e);
            throw e;
        } finally {
            DynamicLogFileGenerator.finishLogger();
        }
    }

    private static void logRequest(CustomRequestWrapper request) {
        try {
            logger.info("Request: {} {} Body: {}", 
                request.getMethod(), 
                request.getRequestURI(),
                request.getBodyAsString());
        } catch (Exception e) {
            logger.error("Error logging request", e);
        }
    }

    private static void logResponse(CustomResponseWrapper response) {
        try {
            logger.info("Response: Status: {} Body: {}", 
                response.getStatus(),
                response.getBodyAsString());
        } catch (Exception e) {
            logger.error("Error logging response", e);
        }
    }
}
