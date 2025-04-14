package com.example.tracing.dbtracing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryGenerator {
    public static String generateSelectQuery(String sql) {

        // update/delete query 에서 select 쿼리로 변환하여 현재값 알기
        sql = sql.toLowerCase().trim();
        if (sql.startsWith("update")) {
            System.out.println("update query!!!!!");
            String table = getTableNameFromUpdateQuery(sql);
            String where = getWhereClause(sql);
            return (table != null && where != null) ? "SELECT * FROM " + table + " " + where : null;
        } else if (sql.startsWith("delete")) {
            System.out.println("delete query!!!!!");
            String table = getTableNameFromDeleteQuery(sql);
            String where = getWhereClause(sql);
            return (table != null && where != null) ? "SELECT * FROM " + table + " " + where : null;
        } else if (sql.startsWith("select")) {
            System.out.println("select query!!!!!");
            return getAllColumnFromSelectQuery(sql);
        } else {
            System.out.println("generated select query fail.");
            return null;
        }
    }

    public static String getAllColumnFromSelectQuery(String sql) {
        if (sql == null) return null;
        try {
            String regex = "(?i)select (.*?) from";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(sql);

            if (matcher.find()) {
                return matcher.replaceAll("select * from");
            }
        } catch (Exception e) {
            System.err.println("[SqlUtils] SELECT * 변환 실패: " + e.getMessage());
        }
        return null;
    }


    public static String getTableNameFromUpdateQuery(String sql) {
        int start = sql.indexOf("update") + 6;
        int end = sql.indexOf("set");
        return (start < end && start > 0 && end > 0) ? sql.substring(start, end).trim() : null;
    }

    public static String getTableNameFromDeleteQuery(String sql) {
        int start = sql.indexOf("delete from") + 11;
        int end = sql.indexOf("where");
        return (start < end && start > 0 && end > 0) ? sql.substring(start, end).trim() : null;
    }

    public static String getWhereClause(String sql) {
        int index = sql.indexOf("where");
        return index > 0 ? sql.substring(index).trim() : null;
    }
}

