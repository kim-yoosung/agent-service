package com.example.tracing.dbtracing;

import com.example.tracing.logging.DynamicLogFileGenerator;
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

import static com.example.tracing.dbtracing.QueryGenerator.generateSelectQuery;

public class PrepareStatementExecuteAdvice {
    public static String previousQuery = "";
    public static final ThreadLocal<Boolean> isInternal = ThreadLocal.withInitial(() -> false);

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.This PreparedStatement stmt,
                               @Advice.Origin Method method,
                               @Advice.AllArguments Object[] args) {
        try {
            System.out.println("[Agent] DB tracing start");
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
                !sql.contains("_seq") &&
                !sql.contains("count(*)") &&
                !sql.equals(previousQuery);
    }

    public static void logQueryDetails(PreparedStatement stmt, Object[] args) {
        if (isInternal.get()) return;
        try {
            isInternal.set(true);
            String sql = SqlUtils.extractSqlFromPreparedStatement(stmt.toString().trim());
            String finalQuery;

            // parameters: [ 가 포함된 경우 → 파라미터 바인딩 직접 수행
            if (sql.contains("parameters: [")) {
                finalQuery = SqlUtils.extractAndBindSql(sql);
            } else {
                finalQuery = sql;
            }

            previousQuery = finalQuery.replaceAll("\\r?\\n", "");
            DynamicLogFileGenerator.log("Executing SQL: " + finalQuery);

            // select query 생성 & Prep query 파일 생성
            String writtenBlobFilePath = generatePreProcessingQuery(stmt, args, sql);
            DynamicLogFileGenerator.log("Prep query: " + writtenBlobFilePath);
        } catch (Exception e) {
            System.out.println("[Agent] 전 쿼리 로깅 실패");
        } finally {
            isInternal.set(false);  // ✅ 플래그 해제
        }
    }

    public static String generatePreProcessingQuery(PreparedStatement stmt, Object[] args, String sql) {
        String writtenBlobFilePath = "";


        if (sql.toLowerCase().contains("insert")) {
            return writtenBlobFilePath;
        }

        // 전 쿼리용 SELECT 생성
        String selectQuery = generateSelectQuery(sql);
        if (selectQuery == null) {
            System.out.println("[Agent] 전 쿼리 SELECT 생성 실패");
            return writtenBlobFilePath;
        }

        DynamicLogFileGenerator.log("Generated Select Query: " + selectQuery);
        try {
            // --- 커넥션 가져오기 로그 추가 --- 
            java.sql.Connection connection = null;
            try {
                System.out.println("[Agent Debug] generatePreProcessingQuery: Attempting to get connection from original statement...");
                connection = stmt.getConnection();
                if (connection != null) {
                    System.out.println("[Agent Debug] generatePreProcessingQuery: Connection obtained successfully. Closed: " + connection.isClosed());
                } else {
                    System.err.println("[Agent Error] generatePreProcessingQuery: stmt.getConnection() returned null!");
                    return ""; // 커넥션 없으면 진행 불가
                }
            } catch (SQLException connEx) {
                System.err.println("[Agent Error] generatePreProcessingQuery: SQLException while getting connection: " + connEx.getMessage());
                connEx.printStackTrace();
                return ""; // 커넥션 얻기 실패 시 진행 불가
            }
            // --- 로그 추가 끝 ---

            System.out.println("[Agent Debug] generatePreProcessingQuery: Preparing select statement using the obtained connection...");
            // 커넥션에서 SELECT용 PreparedStatement 생성
            PreparedStatement selectStmt = connection.prepareStatement(
                    selectQuery,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );

            // 파라미터가 있으면 바인딩
            System.out.println("[Agent - DB] 바인딩 직전");
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    selectStmt.setObject(i + 1, args[i]);
                }
            }
            else {
                System.out.println("[Agent - DB] 파라미터가 없음");
            }

            System.out.println("[Agent - DB] 쿼리 실행 직전");

            // 쿼리 실행
            ResultSet rs = selectStmt.executeQuery();
            System.out.println("[Agent - DB] 쿼리 실행 직후");


            // 결과가 없으면 스킵
            rs.last();
            int rowCount = rs.getRow();
            if (rowCount == 0) {
                System.out.println("[Agent - DB] rowCount 0");
                return "";
            }
            System.out.println("[Agent - DB] beforeFirst 0");

            rs.beforeFirst();
            System.out.println("[Agent - DB] beforeFirst 1");


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

