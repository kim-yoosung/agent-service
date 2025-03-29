package com.example.tracing.dbtracing;

public class SqlUtils {
    public static String extractSqlFromPreparedStatement(String proxyToString) {
        int startIndex = proxyToString.indexOf(": ") + 2;
        if (startIndex <= 1) return proxyToString;

        String sql = proxyToString.substring(startIndex).trim();

        int commentStartIndex = sql.indexOf("/*");
        int commentEndIndex = sql.indexOf("*/") + 2;
        if (commentStartIndex >= 0 && commentEndIndex > commentStartIndex) {
            sql = sql.substring(0, commentStartIndex).trim() + " " +
                    sql.substring(commentEndIndex).trim();
        }

        return sql.replaceAll("\\r?\\n", " ");
    }
}