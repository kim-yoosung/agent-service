//package com.example.tracing.dbtracing;
//
//public class SqlUtils {
//    public static String extractSqlFromPreparedStatement(String proxyToString) {
//        String sql = proxyToString;
//        int startIndex = sql.indexOf(": ") + 2;
//        if (startIndex > 1) {
//            sql = proxyToString.substring(startIndex).trim();
//        }
//
//        // 주석 제거
//        int commentStartIndex = sql.indexOf("/*");
//        int commentEndIndex = sql.indexOf("*/") + 2;
//        if (commentStartIndex >= 0 && commentEndIndex > commentStartIndex) {
//            sql = sql.substring(0, commentStartIndex).trim() + " " + sql.substring(commentEndIndex).trim();
//        }
//
//        return sql;
//    }
//}
