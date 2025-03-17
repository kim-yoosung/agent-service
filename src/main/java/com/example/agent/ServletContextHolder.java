package com.example.agent;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.logging.Logger;

public class ServletContextHolder implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(ServletContextHolder.class.getName());
    private static ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        servletContext = sce.getServletContext();
        if (servletContext != null) {
            logger.info("[Agent] 🚀 ServletContext 초기화 성공: " + servletContext.getClass().getName());
        } else {
            logger.severe("[Agent] ❌ ServletContext 초기화 실패! sce.getServletContext()가 null");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        servletContext = null;
        logger.info("[Agent] ServletContext 제거됨!");
    }

    public static ServletContext getServletContext() {
        if (servletContext == null) {
            logger.severe("[Agent] ServletContext가 아직 초기화되지 않음!");
        }
        return servletContext;
    }

    // ✅ 🚀 강제 등록 메서드 추가
    public static void forceRegister(ServletContext context) {
        if (servletContext == null) {
            servletContext = context;
            logger.info("[Agent] ServletContext 강제 등록됨!");
        }
    }


}
