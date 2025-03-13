package com.example.agent;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.Callable;

public class FilterInterceptor {
    @SuppressWarnings("unused")
    public static void intercept(@AllArguments Object[] args, @SuperCall Callable<Void> originalCall) throws Exception {
        ServletRequest request = (ServletRequest) args[0];
        ServletResponse response = (ServletResponse) args[1];
        FilterChain chain = (FilterChain) args[2];

        // ✅ 요청 Wrapping
        if (request instanceof HttpServletRequest) {
            request = new CustomRequestWrapper((HttpServletRequest) request);
        }

        // ✅ 응답 Wrapping
        if (response instanceof HttpServletResponse) {
            response = new CustomResponseWrapper((HttpServletResponse) response);
        }

        // ✅ 기존 doFilter() 실행
        originalCall.call();
    }
}
