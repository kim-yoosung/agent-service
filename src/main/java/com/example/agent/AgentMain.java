package com.example.agent;

import com.example.tracing.apitracing.DispatcherServletAdvice;
import com.example.tracing.dbtracing.PrepareStatementExecuteAdvice;
import com.example.tracing.logging.DynamicLogFileGenerator;
import com.example.tracing.outgingtracing.RestTemplateInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.dynamic.DynamicType;

import java.lang.instrument.Instrumentation;
import java.sql.PreparedStatement;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class AgentMain {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] 🚀 자바 에이전트 시작됨, Spring Boot 실행 대기 중...");

        // 로그 확인용 Listener 추가
        AgentBuilder.Listener listener = new AgentBuilder.Listener.Adapter() {
            @Override
            public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
                if (typeName.contains("DispatcherServlet")) {
                    System.out.println("[Agent] 🔍 발견된 클래스: " + typeName + " | loaded: " + loaded);
                }
            }

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
                                         JavaModule module, boolean loaded, DynamicType dynamicType) {
                System.out.println("[Agent] ✅ 후킹 성공: " + typeDescription.getName());
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                System.err.println("[Agent] ❌ 후킹 에러 발생: " + typeName);
                throwable.printStackTrace();
            }
        };

        DynamicLogFileGenerator.initLogger();
        DynamicLogFileGenerator.log("[Agent] 🚀 자바 에이전트 시작됨");
        DynamicLogFileGenerator.finishLogger();

        // DispatcherServlet 후킹
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(listener) // 여기서 연결
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
                .type(hasSuperType(named("org.springframework.web.client.RestTemplate")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(named("doExecute"))
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
    }
}
