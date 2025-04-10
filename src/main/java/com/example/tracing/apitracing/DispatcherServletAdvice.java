package com.example.tracing.apitracing;

import com.example.tracing.dto.WireMockReqDTO;
import com.example.tracing.dto.WireMockResDTO;
import com.example.tracing.dto.WiremockDTO;
import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.example.tracing.apitracing.IncomingReqResFilter.*;

public class DispatcherServletAdvice {

    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomRequestWrapper> requestWrapperHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(value = 0, readOnly = false) HttpServletRequest request,
                               @Advice.Argument(value = 1, readOnly = false) HttpServletResponse response) {
        try {
            DynamicLogFileGenerator.initLogger();
            System.out.println("[Agent] DispatcherServletAdvice start");

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

}
