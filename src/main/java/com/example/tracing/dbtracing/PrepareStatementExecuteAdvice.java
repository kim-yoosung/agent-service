package com.example.tracing.dbtracing;

import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static com.example.tracing.dbtracing.PrepareStatementExecuteHandler.exportToFile;
import static com.example.tracing.dbtracing.QueryGenerator.generateSelectQuery;

public class PrepareStatementExecuteAdvice {
    public static String previousQuery = "";
    public static final ThreadLocal<Boolean> isInternal = ThreadLocal.withInitial(() -> false);

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.This PreparedStatement stmt,
                               @Advice.Origin Method method,
                               @Advice.AllArguments Object[] args) {
        try {
            boolean isTarget = isTargetQuery(stmt);

            if (isTarget) {
                boolean needRollback = needRollback(method);
                logQueryDetails(stmt, args); // 전 쿼리용 SELECT 실행 및 row 수 로깅

                if (needRollback) {
                    stmt.getConnection().rollback();
                } else {
                    stmt.getConnection().commit();
                }
            }
        } catch (Exception e) {
            System.out.println("[Agent] Advice 실행 중 예외 발생");
        }
    }

    public static boolean needRollback(Method method) {
        return !method.getName().equals("executeQuery");
    }

//    public static boolean isInRequestScope() {
//        return RequestContextHolder.getRequestAttributes() != null;
//    }

    public static boolean isTargetQuery(PreparedStatement stmt) {
        String sql = SqlUtils.extractSqlFromPreparedStatement(stmt.toString()).toLowerCase();
        return !sql.contains("hibernate_sequence") &&
                !sql.contains(".message") &&
                !sql.contains("_seq") &&
                !sql.contains("count(*)") &&
                !sql.equals(previousQuery);
    }

    public static void logQueryDetails(PreparedStatement stmt, Object[] args) {
        if (isInternal.get()) return;
        try {
            isInternal.set(true);
            String sql = SqlUtils.extractSqlFromPreparedStatement(stmt.toString());
            previousQuery = sql.replaceAll("\\r?\\n", "");
            DynamicLogFileGenerator.log("[Agent] Executing SQL: " + previousQuery);

            // 전 쿼리 SELECT 생성 및 실행, 파일 경로 반환
            String writtenBlobFilePath = generatePreProcessingQuery(stmt, args, sql);

            // 마지막에 로그 출력은 여기서
            DynamicLogFileGenerator.log("Prep query: " + writtenBlobFilePath);

        } catch (Exception e) {
            System.out.println("[Agent] 전 쿼리 로깅 실패");
        } finally {
            isInternal.set(false);  // ✅ 플래그 해제
        }
    }

    public static String generatePreProcessingQuery(PreparedStatement stmt, Object[] args, String sql) {
        String writtenBlobFilePath = "";

        try {
            if (sql.toLowerCase().contains("insert")) {
                return writtenBlobFilePath;
            }

            // 전 쿼리용 SELECT 생성
            String selectQuery = generateSelectQuery(sql);
            if (selectQuery == null) {
                System.out.println("[Agent] 전 쿼리 SELECT 생성 실패");
                return writtenBlobFilePath;
            }

            DynamicLogFileGenerator.log("[Agent] Generated Select Query: " + selectQuery);

            try (PreparedStatement selectStmt = stmt.getConnection().prepareStatement(
                    selectQuery,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY)) {

                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        selectStmt.setObject(i + 1, args[i]);
                    }
                }

                try (ResultSet rs = selectStmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int rsLength = 0;
                    while (rs.next()) rsLength++;

                    rs.beforeFirst();
                    System.out.println("ResultSet has " + rsLength + " rows before populate()");
                    System.out.println("ResultSet is " + rs);

                    if (rsLength > 0) {
                        String fileName = "blob-" + System.currentTimeMillis() + ".dat";
                        File blobFile = new File("logs/", fileName);
                        writtenBlobFilePath = exportToFile(blobFile, rs);
                    }
                }

            }

        } catch (Exception e) {
            System.out.println("[Agent] 전 쿼리 처리 실패: " + e.getMessage());
        }

        return writtenBlobFilePath;
    }

}

