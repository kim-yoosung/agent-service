package com.example.agent;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

public class AgentMain {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("🌟 API Request/Response Wrapper Agent Started!");

        new AgentBuilder.Default()
                .type(ElementMatchers.hasSuperType(ElementMatchers.named("javax.servlet.Filter"))) // Filter 인터페이스 구현체 감지
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("doFilter")) // doFilter() 메서드 감지
                                .intercept(Advice.to(FilterInterceptor.class))) // FilterInterceptor 적용
                .installOn(inst);
    }
}
