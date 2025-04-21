package com.example.agent;

import com.example.tracing.apitracing.DispatcherServletAdvice;
import com.example.tracing.dbtracing.PrepareStatementExecuteAdvice;
import com.example.tracing.logging.DynamicLogFileGenerator;
import com.example.tracing.outgingtracing.RestTemplateInterceptor;
import com.example.tracing.sockettracing.SocketInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.sql.PreparedStatement;
import java.util.jar.JarFile;

import static java.lang.System.exit;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentMain {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] 🚀 자바 에이전트 시작됨, Spring Boot 실행 대기 중...");

        System.out.println("[Agent] Jar file path: " + AgentConfig.getOwnJarPath());

        appendToBootstrap(inst);


        DynamicLogFileGenerator.initLogger();
        DynamicLogFileGenerator.log("[Agent] 🚀 자바 에이전트 시작됨");
        DynamicLogFileGenerator.finishLogger();

        // DispatcherServlet 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(ElementMatchers.none())
                .type(named("org.springframework.web.servlet.DispatcherServlet"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(named("doDispatch"))
                                .intercept(Advice.to(DispatcherServletAdvice.class))
                )
                .installOn(inst);

        // OutgoingHttp 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.hasSuperType(named("org.springframework.web.client.RestTemplate")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("doExecute"))
                                .intercept(MethodDelegation.to(RestTemplateInterceptor.class))
                )
                .installOn(inst);

        // DB Connection 후킹
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

        // Socket 후킹
        new AgentBuilder.Default()
                .ignore(none())
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(named("java.net.Socket"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder
                                .visit(Advice.to(SocketInterceptor.class).on(named("connect")))
                )
                .installOn(inst);
    }

    private static void appendToBootstrap(Instrumentation inst) {
        try {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(AgentConfig.getOwnJarPath()));
        } catch (IOException e) {
            System.out.println("[Agent] Failed to load jar file: " + AgentConfig.getOwnJarPath() + ", " + e.getMessage());
            exit(1);
        }
    }
}