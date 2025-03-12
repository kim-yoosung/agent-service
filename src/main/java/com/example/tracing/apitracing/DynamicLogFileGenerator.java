package com.example.tracing.apitracing;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicLogFileGenerator {
    private static FileAppender dynamicFileAppender = null;
    private static final Logger logger = LoggerFactory.getLogger(DynamicLogFileGenerator.class);

    // 🌟 새로운 로그 파일 생성
    public static void initDynamicLogger() {
        String fileName = "logs/transaction-" + System.currentTimeMillis() + ".log";
        System.out.println("[Agent] 새로운 로그 파일 생성: " + fileName);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        dynamicFileAppender = new FileAppender<>();
        dynamicFileAppender.setContext(loggerContext);
        dynamicFileAppender.setName("DYNAMIC_FILE_APPENDER");
        dynamicFileAppender.setFile(fileName);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        dynamicFileAppender.setEncoder(encoder);
        dynamicFileAppender.start();

        ((ch.qos.logback.classic.Logger) logger).addAppender(dynamicFileAppender);
        logger.info("[Agent] 트랜잭션 로그 시작");
    }

    // 🌟 로그 파일 닫기
    public static void finishDynamicLogger() {
        if (dynamicFileAppender != null) {
            logger.info("[Agent] 트랜잭션 로그 종료");
            ((ch.qos.logback.classic.Logger) logger).detachAppender(dynamicFileAppender);
            dynamicFileAppender.stop();
            System.out.println("[Agent] 로그 파일 닫음!");
            dynamicFileAppender = null;
        }
    }
}
