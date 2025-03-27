package com.example.tracing.dbtracing;

import java.sql.PreparedStatement;

import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;

public class PrepareStatementAdvice {


    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin String method) {
        System.out.println("[Agent] Entering method: " + method);
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Return PreparedStatement stmt) {
        try {
            String rawSql = stmt.toString();
            String parsedSql = SqlUtils.extractSqlFromPreparedStatement(rawSql);
            DynamicLogFileGenerator.log("[Agent - DB] SQL: " + parsedSql);
        } catch (Exception e) {
            System.err.println("[Agent] SQL 로깅 실패: " + e.getMessage());
        }
    }

}
