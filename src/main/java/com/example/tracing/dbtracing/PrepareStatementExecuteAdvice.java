package com.example.tracing.dbtracing;

import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import static com.example.tracing.dbtracing.QueryGenerator.generateSelectQuery;

public class PrepareStatementExecuteAdvice {
    private static final Logger logger = LoggerFactory.getLogger(PrepareStatementExecuteAdvice.class);
    public static String previousQuery = "";
    public static final ThreadLocal<Boolean> isInternal = ThreadLocal.withInitial(() -> false);

    @RuntimeType
    public static Object intercept(@This Object obj,
                                 @Origin Method method,
                                 @AllArguments Object[] args,
                                 @SuperCall Callable<?> callable) throws Exception {
        PreparedStatement stmt = (PreparedStatement) obj;
        try {
            // 쿼리 실행 전 처리
            onEnter(stmt, method, args);

            // 원래 메서드 실행
            Object result = callable.call();

            // 쿼리 실행 후 처리
            if (result instanceof ResultSet && isTargetQuery(stmt)) {
                ResultSet rs = (ResultSet) result;
                File tempFile = File.createTempFile("query_result_", ".csv");
                String filePath = exportToFile(tempFile, rs);
                logger.info("[Agent] Query result exported to: {}", filePath);
            }

            return result;
        } catch (Exception e) {
            logger.error("[Agent] Error in SQL interception", e);
            throw e;
        }
    }

    private static void onEnter(PreparedStatement stmt, Method method, Object[] args) {
        try {
            if (needRollback(method)) {
                logger.info("[Agent] Skipping non-select query: {}", method.getName());
                return;
            }

            String query = generatePreProcessingQuery(stmt, args, method.getName());
            logger.info("[Agent] Executing query: {}", query);
        } catch (Exception e) {
            logger.error("[Agent] Error in query preprocessing", e);
        }
    }

    private static boolean needRollback(Method method) {
        String methodName = method.getName().toLowerCase();
        return methodName.contains("update") || 
               methodName.contains("delete") || 
               methodName.contains("insert");
    }

    private static boolean isTargetQuery(PreparedStatement stmt) {
        try {
            String sql = stmt.toString().toLowerCase();
            return sql.contains("select") && !sql.contains("dual");
        } catch (Exception e) {
            return false;
        }
    }

    private static String generatePreProcessingQuery(PreparedStatement stmt, Object[] args, String methodName) {
        try {
            StringBuilder query = new StringBuilder();
            query.append("Method: ").append(methodName).append("\n");
            query.append("SQL: ").append(stmt.toString()).append("\n");
            
            if (args != null && args.length > 0) {
                query.append("Parameters: ");
                for (int i = 0; i < args.length; i++) {
                    query.append(args[i]).append(", ");
                }
            }
            
            return query.toString();
        } catch (Exception e) {
            return "Failed to generate query: " + e.getMessage();
        }
    }

    private static String exportToFile(File file, ResultSet rs) throws SQLException, IOException {
        try (FileWriter writer = new FileWriter(file)) {
            if (rs == null || !rs.next()) {
                return file.getAbsolutePath();
            }

            // Write headers
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                writer.append(rs.getMetaData().getColumnName(i));
                if (i < columnCount) writer.append(',');
            }
            writer.append('\n');

            // Write data
            do {
                for (int i = 1; i <= columnCount; i++) {
                    writer.append(rs.getString(i));
                    if (i < columnCount) writer.append(',');
                }
                writer.append('\n');
            } while (rs.next());

            return file.getAbsolutePath();
        }
    }
}


