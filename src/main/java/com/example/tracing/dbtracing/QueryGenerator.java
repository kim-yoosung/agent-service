package com.example.tracing.dbtracing;

public class QueryGenerator {
    public static String generateSelectQuery(String sql) {
        sql = sql.toLowerCase();
        if (sql.startsWith("update")) {
            String table = getTableNameFromUpdate(sql);
            String where = getWhereClause(sql);
            return (table != null && where != null) ? "SELECT * FROM " + table + " " + where : null;
        } else if (sql.startsWith("delete")) {
            String table = getTableNameFromDelete(sql);
            String where = getWhereClause(sql);
            return (table != null && where != null) ? "SELECT * FROM " + table + " " + where : null;
        } else if (sql.startsWith("select")) {
            return sql;
        }
        return null;
    }

    private static String getTableNameFromUpdate(String sql) {
        int start = sql.indexOf("update") + 6;
        int end = sql.indexOf("set");
        return (start < end && start > 0 && end > 0) ? sql.substring(start, end).trim() : null;
    }

    private static String getTableNameFromDelete(String sql) {
        int start = sql.indexOf("delete from") + 11;
        int end = sql.indexOf("where");
        return (start < end && start > 0 && end > 0) ? sql.substring(start, end).trim() : null;
    }

    private static String getWhereClause(String sql) {
        int index = sql.indexOf("where");
        return index > 0 ? sql.substring(index).trim() : null;
    }
}

