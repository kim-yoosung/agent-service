package com.example.tracing.dbtracing;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;

import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;

public class PrepareStatementAdvice {

    @Advice.OnMethodExit
    public static void onExit(@Advice.Return(readOnly = false) PreparedStatement stmt) {
        try {
            System.out.println("sql advice start!!!!");
            String rawSql = stmt.toString();
            String parsedSql = SqlUtils.extractSqlFromPreparedStatement(rawSql);

            DynamicLogFileGenerator.log("[Agent - DB] SQL 실행: " + parsedSql);

        } catch (Exception e) {
            System.err.println("[Agent - DB] 프록시 생성 실패: " + e.getMessage());
        }


    }
}
