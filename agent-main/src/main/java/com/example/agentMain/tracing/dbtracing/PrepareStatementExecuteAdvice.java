package com.example.agentMain.tracing.dbtracing;

import com.example.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrepareStatementExecuteAdvice {
    public static String previousQuery = "";
    public static final ThreadLocal<Boolean> isInternal = ThreadLocal.withInitial(() -> false);

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.This PreparedStatement stmt,
                               @Advice.Origin Method method,
                               @Advice.AllArguments Object[] args) {
        try {
            // 현재 트랜잭션 ID 가져오기
            String txId = DynamicLogFileGenerator.getCurrentTransactionId();
            DynamicLogFileGenerator.setCurrentTransaction(txId);
            
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

    public static boolean isTargetQuery(PreparedStatement stmt) {
        String sql = SqlUtils.extractSqlFromPreparedStatement(stmt.toString()).toLowerCase();
        return !sql.contains("hibernate_sequence") &&
                !sql.contains(".message") &&
                !sql.contains("_SEQ") &&
                !sql.contains("count(*)") &&
                !sql.contains("wf.") &&
                !sql.contains("ev.") &&
                !sql.equals(previousQuery);
    }

    public static void logQueryDetails(PreparedStatement stmt, Object[] args) {
        if (isInternal.get()) return;
        try {
            isInternal.set(true);
            String sql = SqlUtils.extractSqlFromPreparedStatement(stmt.toString().trim());
            String finalQuery;

            // parameters: [ 가 포함된 경우 → 파라미터 바인딩 직접 수행
            if (sql.contains("parameters : [")) {
                finalQuery = SqlUtils.extractAndBindSql(sql);
            } else {
                finalQuery = sql;
            }

            previousQuery = finalQuery.replaceAll("\\r?\\n", "");
            DynamicLogFileGenerator.log("Executing SQL: " + finalQuery);

            // select query 생성 & Prep query 파일 생성
            String writtenBlobFilePath = generatePreProcessingQuery(stmt, args, finalQuery);
            DynamicLogFileGenerator.log("Prep query: " + writtenBlobFilePath);
        } catch (Exception e) {
            System.out.println("[Agent] 전 쿼리 로깅 실패");
        } finally {
            isInternal.set(false);
        }
    }

    public static String generatePreProcessingQuery(PreparedStatement stmt, Object[] args, String sql) {
        String writtenBlobFilePath = "";

        if (sql.toLowerCase().contains("insert")) {
            return writtenBlobFilePath;
        }

        // 전 쿼리용 SELECT 생성
        String selectQuery = QueryGenerator.generateSelectQuery(sql);
        if (selectQuery == null) {
            return writtenBlobFilePath;
        }

        DynamicLogFileGenerator.log("Generated Select Query: " + selectQuery);
        try {
            // --- 커넥션 가져오기 로그 추가 --- 
            java.sql.Connection connection = null;
            try {
                connection = stmt.getConnection();
                if (connection != null) {
                } else {
                    return ""; // 커넥션 없으면 진행 불가
                }
            } catch (SQLException connEx) {
                connEx.printStackTrace();
                return ""; // 커넥션 얻기 실패 시 진행 불가
            }
            // --- 로그 추가 끝 ---

            // 커넥션에서 SELECT용 PreparedStatement 생성
            PreparedStatement selectStmt = connection.prepareStatement(
                    selectQuery,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );

            // 파라미터가 있으면 바인딩
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    selectStmt.setObject(i + 1, args[i]);
                }
            }
            else {
                System.out.println("[Agent - DB] 파라미터가 없음");
            }

            // 쿼리 실행
            ResultSet rs = selectStmt.executeQuery();

            // 결과가 없으면 스킵
            rs.last();
            int rowCount = rs.getRow();
            if (rowCount == 0) {
                System.out.println("[Agent - DB] rowCount 0");
                return "";
            }
            rs.beforeFirst();

            // 파일 경로 생성
            String fileName = "blob-" + System.currentTimeMillis() + ".dat";
            File outFile = new File("logs", fileName);
            outFile.getParentFile().mkdirs(); // logs 디렉토리 없을 경우 생성

            // ResultSet을 파일로 저장
            writtenBlobFilePath = exportToFile(outFile, rs);

        } catch (Exception e) {
            System.out.println("[Agent] generatePreProcessingQuery 예외 발생: " + e.getMessage());
        }

        return writtenBlobFilePath;
    }
    public static String exportToFile(File outFile, ResultSet rs) throws SQLException, IOException {
        CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
        crs.populate(rs);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outFile))) {
            oos.writeObject(crs);
        }

        return outFile.getAbsolutePath();
    }

}

