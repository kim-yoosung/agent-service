package com.example.agentMain.tracing.apitracing;

import com.example.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;
import java.net.Socket;
import java.net.InetSocketAddress;

public class SocketInterceptor {
    @Advice.OnMethodEnter
    public static void onEnter(@Advice.This Socket socket,
                              @Advice.Argument(0) InetSocketAddress address) {
        try {
            // 현재 트랜잭션 ID 가져오기
            String txId = DynamicLogFileGenerator.getCurrentTransactionId();
            DynamicLogFileGenerator.setCurrentTransaction(txId);
            
            // ... 기존 로직 ...
        } catch (Exception e) {
            System.out.println("[Agent] Socket connect 중 예외 발생");
        }
    }
} 