package com.example.agent;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class AgentMain {
    private static final Logger logger = Logger.getLogger("AgentLogger");
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("🌟 API Request/Response Wrapper Agent Started!");

        new AgentBuilder.Default()
                .type(ElementMatchers.nameContains("Filter"))  // Filter 인터페이스를 구현한 클래스 후킹
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(FilterAdvice.class)
                                .on(ElementMatchers.named("doFilter"))) // doFilter 메서드 후킹
                ).installOn(inst);
    }
}
