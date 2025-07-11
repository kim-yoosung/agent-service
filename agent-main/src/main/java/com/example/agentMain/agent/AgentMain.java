package com.example.agentMain.agent;

import com.example.agentMain.tracing.apitracing.DispatcherServletAdvice;
import com.example.agentMain.tracing.sockettracing.GetInputStreamAdvice;
import com.example.agentMain.tracing.sockettracing.GetOutputStreamAdvice;
import com.example.agentMain.tracing.sockettracing.SocketInterceptor;
import com.example.agentMain.tracing.dbtracing.PrepareStatementExecuteAdvice;
import com.example.agentMain.tracing.outgoingtracing.RestTemplateInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.util.jar.JarFile;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentMain {
    public static String serviceName;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] 🚀 자바 에이전트 시작됨, Spring Boot 실행 대기 중...");
        System.out.println("[Agent] Socket, isModifiableClass = " + inst.isModifiableClass(Socket.class));
        System.out.println("[Agent] Retransform Supported = " + inst.isRetransformClassesSupported());

        appendToBootstrap(inst);

        if (agentArgs != null) {
            for (String arg : agentArgs.split(",")) {
                String[] keyValue = arg.split("=", 2);
                if (keyValue.length == 2 && "service".equals(keyValue[0])) {
                    serviceName = keyValue[1].toLowerCase();
                    System.out.println("Service name is: " + serviceName);
                }
            }
        }

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
                                .visit(Advice.to(GetInputStreamAdvice.class).on(named("getInputStream")))
                                .visit(Advice.to(GetOutputStreamAdvice.class).on(named("getOutputStream")))
                )
                .installOn(inst);
    }

    private static void appendToBootstrap(Instrumentation inst) {
        try {
            String path = System.getProperty("bootstrapJarPath");
            if (path == null || path.isEmpty()) {
                System.err.println("[Agent] ❌ -DbootstrapJarPath=... 설정이 필요합니다.");
                System.exit(1);
            }
            inst.appendToBootstrapClassLoaderSearch(new JarFile(path));
            System.out.println("[Agent] ✅ bootstrap 등록 완료: " + path);
        } catch (IOException e) {
            System.err.println("[Agent] ❌ bootstrap 등록 실패: " + e.getMessage());
            System.exit(1);
        }
    }
}