package com.example.tracing.outgingtracing;

import net.bytebuddy.implementation.bind.annotation.*;
import org.springframework.http.HttpMethod;


import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.Callable;

public class RestTemplateInterceptor {

    @RuntimeType
    public static Object intercept(@SuperCall Callable<Object> zuper,
                                   @AllArguments Object[] args) throws Exception {

        Object uri = args[0];
        Object httpMethod = args[1];

        System.out.println("📤 [Agent] 요청 URI: " + uri);
        System.out.println("📤 [Agent] HTTP Method: " + (httpMethod != null ? httpMethod.toString() : "null"));

        try {
            Object response = zuper.call();
            System.out.println("📥 [Agent] 응답 성공");
            return response;
        } catch (Exception e) {
            System.out.println("❌ [Agent] 예외 발생: " + e.getMessage());
            throw e;
        }
    }
}