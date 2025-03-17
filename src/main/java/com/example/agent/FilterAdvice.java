package com.example.agent;

import net.bytebuddy.asm.Advice;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public class FilterAdvice {
    private static final Logger logger = Logger.getLogger("AgentLogger");

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(0) ServletRequest request,
                               @Advice.Argument(1) ServletResponse response,
                               @Advice.Argument(2) FilterChain chain) {
        logger.info("[Agent] HTTP 요청 감지: " + ((HttpServletRequest) request).getRequestURI());
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Argument(1) ServletResponse response) {
        logger.info("[Agent] HTTP 응답 완료: " + ((HttpServletResponse) response).getStatus());
    }
}
