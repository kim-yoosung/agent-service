package com.example.agent;

import net.bytebuddy.asm.Advice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DispatcherServletAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.AllArguments Object[] args) {
        if (args.length >= 2 && args[0] instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) args[0];
            String uri = request.getRequestURI();
            String method = request.getMethod();
            String ip = request.getRemoteAddr();
            System.out.println("[Agent] >>> 요청: [" + method + "] " + uri + " from IP: " + ip);
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.AllArguments Object[] args,
                              @Advice.Thrown Throwable throwable) {
        if (args.length >= 2 && args[1] instanceof HttpServletResponse) {
            HttpServletResponse response = (HttpServletResponse) args[1];
            int status = response.getStatus();
            System.out.println("[Agent] <<< 응답 상태: " + status);
        }

        if (throwable != null) {
            System.err.println("[Agent] 예외 발생: " + throwable.getMessage());
        }
    }
}
