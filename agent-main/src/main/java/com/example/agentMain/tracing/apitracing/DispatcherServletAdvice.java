package com.example.agentMain.tracing.apitracing;

import com.example.logging.DynamicLogFileGenerator;
import com.example.agentMain.tracing.dto.WireMockReqDTO;
import com.example.agentMain.tracing.dto.WireMockResDTO;
import com.example.agentMain.tracing.dto.WiremockDTO;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

public class DispatcherServletAdvice {

    private static final String TRANSACTION_ID_HEADER = "X-Transaction-ID";
    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomRequestWrapper> requestWrapperHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(value = 0, typing = Assigner.Typing.DYNAMIC) Object request) {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String transactionId = httpRequest.getHeader(TRANSACTION_ID_HEADER);
            
            if (transactionId == null) {
                transactionId = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            }
            
            DynamicLogFileGenerator.initLogger(transactionId);
            DynamicLogFileGenerator.setCurrentTransaction(transactionId);
        }
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Argument(value = 0, typing = Assigner.Typing.DYNAMIC) Object request,
                             @Advice.Argument(value = 1, typing = Assigner.Typing.DYNAMIC) Object response) {
        try {
            if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                String transactionId = DynamicLogFileGenerator.getCurrentTransactionId();
                if (transactionId != null) {
                    httpResponse.setHeader(TRANSACTION_ID_HEADER, transactionId);
                }
            }
        } finally {
            DynamicLogFileGenerator.finishLogger();
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Thrown Throwable t) {
        try {
            String txId = DynamicLogFileGenerator.getCurrentTransactionId();
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
            System.err.println("[Agent] OnExit error: " + e.getMessage());
        } finally {
            wiremockHolder.remove();
            requestWrapperHolder.remove();
            responseWrapperHolder.remove();
        }
    }

}
