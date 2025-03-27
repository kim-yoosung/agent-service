//package com.example.tracing.dbtracing;
//
//
//import com.example.tracing.logging.DynamicLogFileGenerator;
//
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//import java.sql.PreparedStatement;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Set;
//
//public class PreparedStatementProxyHandler implements InvocationHandler {
//
//    private final PreparedStatement delegate;
//
//    public PreparedStatementProxyHandler(PreparedStatement delegate) {
//        this.delegate = delegate;
//    }
//
//    @Override
//    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//        String methodName = method.getName();
//
//        if (isExecuteMethod(methodName)) {
//            try {
//                String rawSql = delegate.toString(); // MySQL은 toString()에 SQL 포함
//                String parsedSql = SqlUtils.extractSqlFromPreparedStatement(rawSql); // 필요시 추출
//                DynamicLogFileGenerator.log("[Agent - DB] SQL: " + parsedSql);
//            } catch (Exception e) {
//                System.err.println("[Agent - DB] SQL 로깅 실패: " + e.getMessage());
//            }
//        }
//
//        // 실제 메서드 위임
//        return method.invoke(delegate, args);
//    }
//
//    private boolean isExecuteMethod(String methodName) {
//        return methodName.equals("execute") ||
//                methodName.equals("executeQuery") ||
//                methodName.equals("executeUpdate");
//    }
//
//    // 프록시 생성 유틸
//
//    public static PreparedStatement wrap(PreparedStatement stmt) {
//        Class<?>[] interfaces = extractAllInterfaces(stmt.getClass());
//
//        return (PreparedStatement) Proxy.newProxyInstance(
//                stmt.getClass().getClassLoader(),
//                interfaces,
//                new PreparedStatementProxyHandler(stmt)
//        );
//    }
//
//    private static Class<?>[] extractAllInterfaces(Class<?> clazz) {
//        Set<Class<?>> interfaces = new HashSet<>();
//        while (clazz != null) {
//            interfaces.addAll(Arrays.asList(clazz.getInterfaces()));
//            clazz = clazz.getSuperclass();
//        }
//        interfaces.add(PreparedStatement.class);
//        return interfaces.toArray(new Class<?>[0]);
//    }
//}
