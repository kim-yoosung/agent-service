package com.example.agentMain.tracing.outgingtracing;

import net.bytebuddy.implementation.bind.annotation.*;
import java.util.concurrent.Callable;

import static com.example.agentMain.tracing.outgingtracing.OutgingUtils.filterInactiveUrl;

public class RestTemplateInterceptor {

    @RuntimeType
    public static Object intercept(@SuperCall Callable<Object> zuper,
                                   @AllArguments Object[] args) throws Exception {

        Object uri = args[0];
        Object httpMethod = args[1];

        String uriStr = filterInactiveUrl(uri.toString());
        System.out.println("[agent - interceptor] uri " + uriStr);
        System.out.println("[agent - interceptor] httpMethod " + httpMethod);

        String serviceName = OutgingUtils.extractServiceName(uriStr);
        System.out.println("[agent - interceptor] serviceName" + serviceName);

        // if true 인 경우 요청을 빈값으로 보내는 작업 필요
        if (OutgingUtils.shouldSkipRequest(httpMethod.toString(), uriStr, serviceName)) {
            return zuper.call(); // 요청 무시
        }

        // 원래 요청을 그대로 실행
        Object responseObj = zuper.call();
        
        // 응답 감싸기 및 Wiremock 저장
        try {
            if (responseObj != null) {
                // ResponseEntity 타입 체크
                if (responseObj.getClass().getName().equals("org.springframework.http.ResponseEntity")) {

                    // 리플렉션을 사용하여 getBody() 메서드 호출
                    Object body = responseObj.getClass().getMethod("getBody").invoke(responseObj);
                    if (body != null) {
                        // ClientHttpResponseWrapper 생성 시 리플렉션 사용
                        ClientHttpResponseWrapper wrapper = (ClientHttpResponseWrapper) Class.forName("com.example.agentMain.tracing.outgingtracing.ClientHttpResponseWrapper")
                            .getConstructor(Object.class)
                            .newInstance(body);
                        OutgingUtils.handleWiremockLogging(args, wrapper);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[agent - interceptor] Wiremock logging error: " + e.getMessage());
            e.printStackTrace();
        }

        return responseObj;
    }
}