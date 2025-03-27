package com.example.tracing.dbtracing;

import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;

import java.sql.PreparedStatement;

public class PrepareStatementExecuteAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.This PreparedStatement stmt) {
        try {
            // 실행 직전에 toString() 호출 시, 대부분 SQL + 바인딩 값까지 출력
            String fullSql = stmt.toString();

            System.out.println("[Agent - DB] Executing SQL: " + fullSql);
            DynamicLogFileGenerator.log("[Agent - DB] Executing SQL: " + fullSql);

        } catch (Exception e) {
            System.err.println("[Agent - DB] SQL 로깅 실패: " + e.getMessage());
        }
    }
}
