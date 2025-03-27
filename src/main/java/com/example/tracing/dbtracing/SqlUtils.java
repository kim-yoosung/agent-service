package com.example.tracing.dbtracing;

public class SqlUtils {

    public static String extractSqlFromPreparedStatement(String raw) {
        if (raw == null) return "[NO SQL]";

        // 콜론(:)이 없으면 toString 형식이 아니므로 그대로 반환
        int index = raw.indexOf(":");
        if (index == -1 || index + 1 >= raw.length()) {
            return raw;
        }

        // 콜론 이후가 실제 SQL 구문
        return raw.substring(index + 1).trim();
    }
}
