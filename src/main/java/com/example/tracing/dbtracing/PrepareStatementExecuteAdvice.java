package com.example.tracing.dbtracing;

import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PrepareStatementExecuteAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.This PreparedStatement stmt) {
        try {
            String rawSql = stmt.toString();
            String sql = SqlUtils.extractSqlFromPreparedStatement(rawSql);

            System.out.println("[Agent - DB] Executing SQL: " + sql);
            DynamicLogFileGenerator.log("[Agent - DB] Executing SQL: " + sql);
//
//            String selectQuery = QueryGenerator.generateSelectQuery(sql);
//            if (selectQuery == null) {
//                System.out.println("[Agent] 전 쿼리 생성 불가");
//                return;
//            }
//
//            System.out.println("[Agent - DB] 전 쿼리용 SQL: " + selectQuery);
//
//
//            try (PreparedStatement preStmt = stmt.getConnection().prepareStatement(selectQuery)) {
//                for (int i = 0; i < args.length; i++) {
//                    preStmt.setObject(i + 1, args[i]);
//                }
//
//                try (ResultSet rs = preStmt.executeQuery()) {
//                    int count = 0;
//                    while (rs.next()) {
//                        count++;
//                    }
//                    System.out.println("[Agent] 전 쿼리 결과 row 수: " + count);
//                }
//            }

        } catch (Exception e) {
            System.err.println("[Agent - DB] SQL 로깅 실패: " + e.getMessage());
        }
    }
}
