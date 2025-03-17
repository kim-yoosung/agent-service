package com.example.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;
import jakarta.servlet.*;

public class AgentMain {
    private static final Logger logger = Logger.getLogger(AgentMain.class.getName());
    ServletContext servletContext = ServletContextHolder.getServletContext();

    public static void premain(String agentArgs, Instrumentation inst) {
        logger.info("[Agent] 🚀 에이전트 시작됨, ServletContext 찾기 시작...");

        new Thread(() -> {
            try {
                int attempts = 0;
                while (attempts < 20) {  // 🚀 최대 20초 동안 대기
                    ServletContext context = findServletContext();
                    if (context != null) {
                        logger.info("[Agent] ✅ ServletContext 발견됨! 필터 등록 가능.");
                        ServletContextHolder.forceRegister(context); // 🚀 강제로 등록
                        break;
                    }

                    Thread.sleep(1000);  // 🚀 1초 대기 후 다시 확인
                    attempts++;
                }

                if (ServletContextHolder.getServletContext() == null) {
                    logger.severe("[Agent] ❌ 20초가 지나도 ServletContext를 찾지 못함!");
                }

            } catch (Exception e) {
                logger.severe("[Agent] ❌ ServletContext 찾는 중 오류 발생: " + e.getMessage());
            }
        }).start();
    }

    private static ServletContext findServletContext() {
        try {
            logger.info("[Agent] 🚀 `ServletContext` 찾기 시작...");

            // ✅ `ServletContextHolder`에서 `ServletContext` 가져오기 시도
            ServletContext servletContext = ServletContextHolder.getServletContext();
            if (servletContext == null) {
                logger.severe("[Agent] ❌ `ServletContextHolder`에서 `ServletContext`를 가져올 수 없음!");
                return null;
            }

            logger.info("[Agent] ✅ `ServletContext` 발견됨: " + servletContext.getClass().getName());
            return servletContext;
        } catch (Exception e) {
            logger.severe("[Agent] ❌ `ServletContext` 찾는 중 오류 발생: " + e.getMessage());
        }
        return null;
    }


}
