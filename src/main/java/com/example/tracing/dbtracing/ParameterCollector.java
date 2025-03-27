package com.example.tracing.dbtracing;

import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ParameterCollector {
    private static final Map<PreparedStatement, Map<Integer, Object>> paramMap = new WeakHashMap<>();

    public static synchronized void addParameter(PreparedStatement stmt, Object index, Object value) {
        paramMap.computeIfAbsent(stmt, k -> new HashMap<>())
                .put((Integer) index, value);
    }

    public static synchronized Map<Integer, Object> getParameters(PreparedStatement stmt) {
        return paramMap.getOrDefault(stmt, Collections.emptyMap());
    }
}
