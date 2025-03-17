package com.example.agent;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class AgentFilter implements Filter {
    private static final Logger logger = Logger.getLogger(AgentFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("[Agent] 에이전트 필터 초기화됨");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        logger.info("[Agent] HTTP 요청 감지: " + ((HttpServletRequest) request).getRequestURI());

        chain.doFilter(request, response);

        logger.info("[Agent] HTTP 응답 완료: " + ((HttpServletResponse) response).getStatus());
    }

    @Override
    public void destroy() {
        logger.info("[Agent] 에이전트 필터 제거됨");
    }
}
