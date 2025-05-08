package com.example.agentMain.tracing.apitracing;

import com.example.logging.DynamicLogFileGenerator;
import com.example.agentMain.tracing.dto.WireMockReqDTO;
import com.example.agentMain.tracing.dto.WireMockResDTO;
import com.example.agentMain.tracing.dto.WiremockDTO;
import net.bytebuddy.asm.Advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DispatcherServletAdvice {

    private static final String TRANSACTION_ID_HEADER = "X-Transaction-ID";
    public static final ThreadLocal<WiremockDTO> wiremockHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomRequestWrapper> requestWrapperHolder = new ThreadLocal<>();
    public static final ThreadLocal<CustomResponseWrapper> responseWrapperHolder = new ThreadLocal<>();

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(value = 0, readOnly = false) HttpServletRequest request,
                               @Advice.Argument(value = 1, readOnly = false) HttpServletResponse response) {
        try {
            String txId = request.getHeader(TRANSACTION_ID_HEADER);
            
            if (txId == null) {
                DynamicLogFileGenerator.initLogger();
                txId = DynamicLogFileGenerator.getCurrentTransactionId();
            } else {
                DynamicLogFileGenerator.setCurrentTransaction(txId);
            }

            CustomRequestWrapper wrappedRequest = new CustomRequestWrapper(request);
            CustomResponseWrapper wrappedResponse = new CustomResponseWrapper(response);

            // 응답 헤더에도 트랜잭션 ID 추가
            wrappedResponse.setHeader(TRANSACTION_ID_HEADER, txId);

            request = wrappedRequest;
            response = wrappedResponse;

            requestWrapperHolder.set(wrappedRequest);
            responseWrapperHolder.set(wrappedResponse);

        } catch (Exception e) {
            System.err.println("[Agent] OnEnter error: " + e.getMessage());
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
            DynamicLogFileGenerator.finishLogger();
        }
    }

}
