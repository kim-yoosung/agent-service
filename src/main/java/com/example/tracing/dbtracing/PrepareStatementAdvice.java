package com.example.tracing.dbtracing;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;

import net.bytebuddy.asm.Advice;

public class PrepareStatementAdvice {

    @Advice.OnMethodExit
    public static void onExit(@Advice.Return(readOnly = false) PreparedStatement stmt) {

        System.out.println("[DB tracing] >>> 요청");
        // 반환되는 PreparedStatement를 Proxy로 감싸기
        stmt = (PreparedStatement) Proxy.newProxyInstance(
                stmt.getClass().getClassLoader(),
                stmt.getClass().getInterfaces(),
                new PreparedStatementProxyHandler(stmt)
        );
    }
}
