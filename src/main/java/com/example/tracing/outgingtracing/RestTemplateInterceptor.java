package com.example.tracing.outgingtracing;

import net.bytebuddy.implementation.bind.annotation.*;


import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import static com.example.tracing.outgingtracing.OutgingUtils.filterInactiveUrl;

public class RestTemplateInterceptor {

    @RuntimeType
    public static Object intercept(@SuperCall Callable<Object> zuper,
                                   @AllArguments Object[] args) throws Exception {

        Object uri = args[0];
        Object httpMethod = args[2];

        String uriStr = filterInactiveUrl(uri.toString());
        System.out.println("[agent - interceptor] uri " + uriStr);
        System.out.println("[agent - interceptor] httpMethod " + httpMethod);

        String serviceName = OutgingUtils.extractServiceName(uriStr);
        System.out.println("[agent - interceptor] serviceName" + serviceName);

        // if true 인 경우 요청을 빈값으로 보내는 작업 필요
        if (OutgingUtils.shouldSkipRequest(httpMethod.toString(), uriStr, serviceName)) {
            return zuper.call(); // 요청 무시
        }
        Object responseObj = zuper.call(); // 요청 실행

        ClientHttpResponseWrapper wrappedResponse = new ClientHttpResponseWrapper(responseObj);

        OutgingUtils.handleWiremockLogging(args, wrappedResponse);

        // 응답 감싸기 및 Wiremock 저장
//        if (responseObj instanceof org.springframework.http.client.ClientHttpResponse) {
//            org.springframework.http.client.ClientHttpResponse original =
//                    (org.springframework.http.client.ClientHttpResponse) responseObj;
//
//            ClientHttpResponseWrapper wrappedResponse = new ClientHttpResponseWrapper(original);
//
//            OutgingUtils.handleWiremockLogging(args, wrappedResponse, uriStr, serviceName);
//            return wrappedResponse;
//        }

        return responseObj;
    }


}