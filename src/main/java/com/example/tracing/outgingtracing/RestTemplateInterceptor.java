package com.example.tracing.outgingtracing;

import net.bytebuddy.implementation.bind.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.ResponseExtractor;
import java.net.URI;
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

        // 원래 요청을 그대로 실행
        Object responseObj = zuper.call();

        // 응답 감싸기 및 Wiremock 저장
        if (responseObj instanceof org.springframework.http.client.ClientHttpResponse) {
            org.springframework.http.client.ClientHttpResponse original =
                    (org.springframework.http.client.ClientHttpResponse) responseObj;

            // 원래 응답을 그대로 사용하면서 로깅만 수행
            OutgingUtils.handleWiremockLogging(args, new ClientHttpResponseWrapper(original));
            return original; // 원래 응답을 그대로 반환
        }

        return responseObj;
    }
}