package com.example.agentMain.tracing.apitracing;

import com.example.logging.DynamicLogFileGenerator;
import com.example.agentMain.tracing.dto.WireMockReqDTO;
import com.example.agentMain.tracing.dto.WireMockResDTO;
import com.example.agentMain.tracing.dto.WiremockDTO;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.StringUtils;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DispatcherServletAdvice {

    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomRequestWrapper> requestWrapperHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(value = 0, readOnly = false) HttpServletRequest request,
                               @Advice.Argument(value = 1, readOnly = false) HttpServletResponse response) {
        try {
            DynamicLogFileGenerator.initLogger();

            System.out.println("[Agent] Incoming Request/Response Filter Initialized");
            DynamicLogFileGenerator.log("Incoming Request/Response Filter Initialized");

            CustomRequestWrapper wrappedRequest = new CustomRequestWrapper(request);
            CustomResponseWrapper wrappedResponse = new CustomResponseWrapper(response);

            request = wrappedRequest;
            response = wrappedResponse;

            requestWrapperHolder.set(wrappedRequest);
            responseWrapperHolder.set(wrappedResponse);

            String referer = wrappedRequest.getHeader("X-testcase-id");
            String testcaseId = StringUtils.isNotBlank(referer) ? referer : "agent_log";
            System.out.println("[Agent - " + Thread.currentThread().getId() + "] testcase id = " + testcaseId);

        } catch (Exception e) {
            System.err.println("[Agent] OnEnter error: " + e.getClass().getName());
            System.err.println("[Agent] OnEnter message: " + e.getMessage());

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            // 콘솔에 출력
            System.err.println("[Agent] OnEnter stacktrace:\n" + exceptionAsString);

            // 로그 파일에도 기록
            DynamicLogFileGenerator.log("OnEnter error: " + e.getClass().getName());
            DynamicLogFileGenerator.log("OnEnter message: " + e.getMessage());
            DynamicLogFileGenerator.log("OnEnter stacktrace:\n" + exceptionAsString);
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Thrown Throwable t) {
        try {
            CustomRequestWrapper requestWrapper = requestWrapperHolder.get();
            CustomResponseWrapper responseWrapper = responseWrapperHolder.get();

            if (requestWrapper == null || responseWrapper == null) return;

            WireMockReqDTO reqDTO = IncomingReqResFilter.getWireMockReqDTO(requestWrapper);
            WireMockResDTO resDTO = new WireMockResDTO();

            WiremockDTO dto = new WiremockDTO();
            dto.setRequest(reqDTO);
            dto.setResponse(resDTO);

            IncomingReqResFilter.captureResponse(responseWrapper, dto);
            IncomingReqResFilter.logWiremockDTO(dto);

        } catch (Exception e) {
            System.err.println("[Agent - " + Thread.currentThread().getId() + "] OnExit error: " + e.getMessage());
        } finally {
            wiremockHolder.remove();
            requestWrapperHolder.remove();
            responseWrapperHolder.remove();
            DynamicLogFileGenerator.finishLogger();
        }
    }

}
