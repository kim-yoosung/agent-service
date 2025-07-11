package com.example.agentMain.tracing.outgoingtracing;

import net.bytebuddy.implementation.bind.annotation.*;
import java.util.concurrent.Callable;

import static com.example.agentMain.tracing.outgoingtracing.OutgoingUtils.filterInactiveUrl;

public class RestTemplateInterceptor {

    @RuntimeType
    public static Object intercept(@SuperCall Callable<Object> zuper,
                                   @AllArguments Object[] args) throws Exception {

        Object uri = args[0];
        Object httpMethod = args[1];

        String uriStr = filterInactiveUrl(uri.toString());
        System.out.println("[agent - interceptor] uri " + uriStr);
        String serviceName = OutgoingUtils.extractServiceName(uriStr);

        // if true 인 경우 요청을 빈값으로 보내는 작업 필요
        if (OutgoingUtils.shouldSkipRequest(httpMethod.toString(), uriStr, serviceName)) {
            return zuper.call(); // 요청 무시
        }

        // 원래 요청을 그대로 실행
        Object responseObj = zuper.call();
        
        // 응답 감싸기 및 Wiremock 저장
        try {
            if (responseObj != null) {
                // ResponseEntity 타입 체크
                if (responseObj.getClass().getName().equals("org.springframework.http.ResponseEntity")) {
                    ClientHttpResponseWrapper wrapper = (ClientHttpResponseWrapper) Class.forName("com.example.agentMain.tracing.outgoingtracing.ClientHttpResponseWrapper")
                        .getConstructor(Object.class)
                        .newInstance(responseObj);
                    OutgoingUtils.handleWiremockLogging(args, wrapper);
                }
            }
        } catch (Exception e) {
            System.out.println("[agent - interceptor] Wiremock logging error: " + e.getMessage());
            e.printStackTrace();
        }

        return responseObj;
    }
}