package com.example.agentMain.tracing.outgingtracing;

import com.example.logging.DynamicLogFileGenerator;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import java.util.concurrent.Callable;

public class RestTemplateInterceptor {
    public static Object intercept(@SuperCall Callable<?> callable) {
        try {
            // 현재 트랜잭션 ID 가져오기
            String txId = DynamicLogFileGenerator.getCurrentTransactionId();
            DynamicLogFileGenerator.setCurrentTransaction(txId);
            
            // ... 기존 로직 ...
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
} 