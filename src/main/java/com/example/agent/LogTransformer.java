package com.example.agent;

import com.example.tracing.apitracing.DynamicLogFileGenerator;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

public class LogTransformer {
    private static final Logger logger = LoggerFactory.getLogger(LogTransformer.class);

    public static void init(Instrumentation inst) {
        new AgentBuilder.Default()
                .type(ElementMatchers.nameContains("Service"))  // 🌟 클래스명에 "Service" 포함된 것만 감시
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                        .method(ElementMatchers.named("save"))
                        .intercept(Advice.to(LoggingAdvice.class))
                )
                .installOn(inst);
    }

    public static class LoggingAdvice {
        @Advice.OnMethodEnter
        public static void onEnter() {
            logger.info("[Agent] 트랜잭션 시작 - 새로운 로그 파일 생성");
            DynamicLogFileGenerator.initDynamicLogger();
        }

        @Advice.OnMethodExit
        public static void onExit() {
            logger.info("[Agent] 트랜잭션 종료 - 로그 파일 닫기");
            DynamicLogFileGenerator.finishDynamicLogger();
        }
    }
}
