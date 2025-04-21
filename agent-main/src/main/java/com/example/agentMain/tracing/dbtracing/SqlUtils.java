package com.example.agentMain.tracing.dbtracing;

import java.util.ArrayList;
import java.util.List;

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

    public static String extractAndBindSql(String raw) {
        try {
            int paramStart = raw.indexOf("parameters : [");
            String sqlPart = raw.substring(0, paramStart).trim();

            System.out.println("[Agent - db] sqlPart!!! "+ sqlPart);

            if (sqlPart.endsWith(",")) {
                sqlPart = sqlPart.substring(0, sqlPart.length() - 1);
            }

            if (sqlPart.startsWith("'") && sqlPart.endsWith("'")) {
                System.out.println("[Agent - db] sqlPart2!!! "+ sqlPart);
                sqlPart = sqlPart.substring(1, sqlPart.length() - 1).trim();
            }

            List<String> params = new ArrayList<>();
            int arrayStart = raw.indexOf('[', paramStart);
            int arrayEnd = raw.indexOf(']', arrayStart);
            if (arrayStart > -1 && arrayEnd > arrayStart) {
                String paramString = raw.substring(arrayStart + 1, arrayEnd);
                String[] paramTokens = paramString.split(",");

                for (String token : paramTokens) {
                    String value = token.trim();
                    if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    params.add(value.replace("'", "''")); // SQL-safe
                }
            }

            return bindParamsToSql(sqlPart, params);

        } catch (Exception e) {
            System.err.println("[Agent] SQL 파라미터 바인딩 실패: " + e.getMessage());
            return raw;
        }
    }
    public static String bindParamsToSql(String sql, List<String> params) {
        StringBuilder result = new StringBuilder();
        int paramIdx = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '?' && paramIdx < params.size()) {
                result.append("'").append(params.get(paramIdx++)).append("'");
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}