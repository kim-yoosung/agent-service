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
            String sql = SqlUtils.extractSqlFromPreparedStatement(stmt.toString());
            previousQuery = sql.replaceAll("\\r?\\n", "");
            DynamicLogFileGenerator.log("Executing SQL: " + previousQuery);

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
            // 커넥션에서 SELECT용 PreparedStatement 생성
            PreparedStatement selectStmt = stmt.getConnection().prepareStatement(
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

            // 쿼리 실행
            ResultSet rs = selectStmt.executeQuery();

            // 결과가 없으면 스킵
            rs.last();
            int rowCount = rs.getRow();
            if (rowCount == 0) {
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

