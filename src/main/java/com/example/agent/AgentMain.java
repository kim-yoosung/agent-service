package com.example.agent;

import java.lang.instrument.Instrumentation;

public class AgentMain {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] 에이전트가 시작되었습니다!");
        LogTransformer.init(inst); // 🌟 ByteBuddy 기반 인터셉터 등록!
    }
}