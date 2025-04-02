package com.example.tracing.outgingtracing;

import net.bytebuddy.implementation.bind.annotation.*;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

public class RestTemplateInterceptor {

    @RuntimeType
    public static Object intercept(@SuperCall Callable<Object> zuper,
                                   @Origin Method method,
                                   @AllArguments Object[] args) throws Exception {

        byte[] body = null;
        try {
            if (args[1] instanceof byte[]) {
                body = (byte[]) args[1];
            }
        } catch (Exception ex) {
            System.out.println("⚠️ body 추출 중 오류: " + ex.getMessage());
        }
        return null;
    }
}
