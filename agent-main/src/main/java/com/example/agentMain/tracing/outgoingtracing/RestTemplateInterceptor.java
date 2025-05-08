package com.example.agentMain.tracing.outgoingtracing;

import com.example.logging.DynamicLogFileGenerator;
import net.bytebuddy.implementation.bind.annotation.*;
import java.util.concurrent.Callable;
import java.lang.reflect.Method;

import static com.example.agentMain.tracing.outgoingtracing.OutgoingUtils.filterInactiveUrl;

public class RestTemplateInterceptor {

    private static final String TRANSACTION_ID_HEADER = "X-Transaction-ID";

    @RuntimeType
    public static Object intercept(@SuperCall Callable<Object> zuper,
                                   @AllArguments Object[] args) throws Exception {
        try {
            String txId = DynamicLogFileGenerator.getCurrentTransactionId();
            
            // 트랜잭션 ID가 있을 때만 헤더에 추가
            if (txId != null && args != null && args.length > 0) {
                Object request = args[0];
                // Reflection을 사용하여 HttpRequest 타입 체크
                if (request != null && request.getClass().getName().contains("HttpRequest")) {
                    try {
                        // Reflection으로 getHeaders 메소드 호출
                        Method getHeadersMethod = request.getClass().getMethod("getHeaders");
                        Object headers = getHeadersMethod.invoke(request);
                        
                        // set 메소드 호출
                        Method setMethod = headers.getClass().getMethod("set", String.class, String.class);
                        setMethod.invoke(headers, TRANSACTION_ID_HEADER, txId);
                    } catch (Exception e) {
                        System.err.println("[RestTemplate] Failed to set header: " + e.getMessage());
                    }
                }
            }

            Object uri = args[0];
            Object httpMethod = args[1];
            String uriStr = filterInactiveUrl(uri.toString());
            String serviceName = OutgoingUtils.extractServiceName(uriStr);

            if (OutgoingUtils.shouldSkipRequest(httpMethod.toString(), uriStr, serviceName)) {
                return zuper.call();
            }

            Object responseObj = zuper.call();
            
            try {
                if (responseObj != null && responseObj.getClass().getName().equals("org.springframework.http.ResponseEntity")) {
                    Object body = responseObj.getClass().getMethod("getBody").invoke(responseObj);
                    if (body != null) {
                        ClientHttpResponseWrapper wrapper = (ClientHttpResponseWrapper) Class.forName("com.example.agentMain.tracing.outgoingtracing.ClientHttpResponseWrapper")
                            .getConstructor(Object.class)
                            .newInstance(body);
                        OutgoingUtils.handleWiremockLogging(args, wrapper);
                    }
                }
            } catch (Exception e) {
                System.err.println("[RestTemplate] Failed to handle wiremock logging: " + e.getMessage());
            }

            return responseObj;
        } catch (Exception e) {
            System.err.println("[RestTemplate] Exception during request: " + e.getMessage());
            throw e;
        }
    }
}