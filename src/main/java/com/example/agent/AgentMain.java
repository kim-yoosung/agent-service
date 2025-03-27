package com.example.agent;

import com.example.tracing.apitracing.DispatcherServletAdvice;
import com.example.tracing.dbtracing.PrepareStatementExecuteAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.sql.PreparedStatement;
import java.util.logging.Logger;

public class AgentMain {
    private static final Logger logger = Logger.getLogger(AgentMain.class.getName());

    public static void premain(String agentArgs, Instrumentation inst) {
        logger.info("[Agent] 🚀 자바 에이전트 시작됨, Spring Boot 실행 대기 중...");

        // 1. DispatcherServlet.doDispatch() 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(ElementMatchers.none())
                .type(ElementMatchers.named("org.springframework.web.servlet.DispatcherServlet"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("doDispatch"))
                                .intercept(Advice.to(DispatcherServletAdvice.class))
                )
                .installOn(inst);

        // 2. java.sql.Connection.prepareStatement() 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(ElementMatchers.none())
                .type(ElementMatchers.isSubTypeOf(PreparedStatement.class))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("execute")
                                        .or(ElementMatchers.named("executeQuery"))
                                        .or(ElementMatchers.named("executeUpdate")))
                                .intercept(Advice.to(PrepareStatementExecuteAdvice.class))
                )
                .installOn(inst);
    }
}
