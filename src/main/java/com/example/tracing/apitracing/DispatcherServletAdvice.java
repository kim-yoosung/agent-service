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

    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomRequestWrapper> requestWrapperHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();

    @RuntimeType
    public static Object intercept(@Origin Method method,
                                 @This Object target,
                                 @AllArguments Object[] args,
                                 @SuperCall Callable<?> callable) throws Exception {
        try {
            DynamicLogFileGenerator.initLogger();
            System.out.println("[Agent] DispatcherServletAdvice start");

            HttpServletRequest request = (HttpServletRequest) args[0];
            HttpServletResponse response = (HttpServletResponse) args[1];

            CustomRequestWrapper wrappedRequest = new CustomRequestWrapper(request);
            CustomResponseWrapper wrappedResponse = new CustomResponseWrapper(response);

            requestWrapperHolder.set(wrappedRequest);
            responseWrapperHolder.set(wrappedResponse);

            // 원래 메서드 호출
            Object result = callable.call();

            // 응답 처리
            CustomRequestWrapper requestWrapper = requestWrapperHolder.get();
            CustomResponseWrapper responseWrapper = responseWrapperHolder.get();

            if (requestWrapper != null && responseWrapper != null) {
                WireMockReqDTO reqDTO = getWireMockReqDTO(requestWrapper);
                WireMockResDTO resDTO = new WireMockResDTO();
                WiremockDTO dto = new WiremockDTO();
                dto.setRequest(reqDTO);
                dto.setResponse(resDTO);
                captureResponse(responseWrapper, dto);
                logWiremockDTO(dto);
            }

            return result;

        } catch (Exception e) {
            System.err.println("[Agent] Intercept 오류: " + e.getMessage());
            throw e;
        } finally {
            wiremockHolder.remove();
            requestWrapperHolder.remove();
            responseWrapperHolder.remove();
            DynamicLogFileGenerator.finishLogger();
        }
    }
}
