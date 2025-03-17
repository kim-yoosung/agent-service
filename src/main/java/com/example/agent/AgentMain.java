package com.example.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;
import jakarta.servlet.*;

public class AgentMain {
    private static final Logger logger = Logger.getLogger(AgentMain.class.getName());

    public static void premain(String agentArgs, Instrumentation inst) {
        logger.info("[Agent] 에이전트 시작됨, 필터 등록 준비");

        new Thread(() -> {
            try {
                // 🚀 서블릿 컨텍스트가 준비될 때까지 대기
                while (true) {
                    ServletContext context = ServletContextHolder.getServletContext();
                    if (context != null) {
                        logger.info("[Agent] 서블릿 컨텍스트 발견됨! 필터 등록 시작");

                        // 🚀 에이전트 필터 등록 (jakarta.servlet.Filter 사용)
                        FilterRegistration.Dynamic agentFilter = context.addFilter("agentFilter", new AgentFilter());
                        agentFilter.addMappingForUrlPatterns(null, false, "/*");

                        logger.info("[Agent] 에이전트 필터가 등록됨!");
                        break;
                    }
                    logger.info("[Agent] context null");
                    Thread.sleep(1000); // 🚀 서블릿 컨텍스트가 로딩될 때까지 대기
                }
            } catch (Exception e) {
                logger.severe("[Agent] 필터 등록 중 오류 발생: " + e.getMessage());
            }
        }).start();
    }
}
