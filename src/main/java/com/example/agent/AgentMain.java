package com.example.agent;

import com.example.tracing.apitracing.DispatcherServletAdvice;
import com.example.tracing.outgingtracing.RestTemplateInterceptor;
import com.example.tracing.dbtracing.PrepareStatementExecuteAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.sql.PreparedStatement;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class AgentMain {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] 🚀 자바 에이전트 시작됨, Spring Boot 실행 대기 중...");

        // DispatcherServlet.doDispatch() 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(ElementMatchers.none())
                .type(named("org.springframework.web.servlet.DispatcherServlet"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(named("doDispatch"))
                                .intercept(Advice.to(DispatcherServletAdvice.class))
                )
                .installOn(inst);

//         OutgoingHttp 후킹
        new AgentBuilder.Default()
                .type(ElementMatchers.named("org.springframework.web.client.RestTemplate"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("doExecute"))
                                .intercept(MethodDelegation.to(RestTemplateInterceptor.class))
                )
                .installOn(inst);

        // 2. java.sql.Connection.prepareStatement() 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(ElementMatchers.none())
                .type(ElementMatchers.isSubTypeOf(PreparedStatement.class))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(named("execute")
                                        .or(named("executeQuery"))
                                        .or(named("executeUpdate")))
                                .intercept(Advice.to(PrepareStatementExecuteAdvice.class))
                )
                .installOn(inst);
    }
}
