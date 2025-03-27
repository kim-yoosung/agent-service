//package com.example.tracing.dbtracing;
//
//
//import com.example.tracing.logging.DynamicLogFileGenerator;
//
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.sql.PreparedStatement;
//
//public class PreparedStatementProxyHandler implements InvocationHandler {
//
//    private final PreparedStatement preparedStatement;
//
//    public PreparedStatementProxyHandler(PreparedStatement preparedStatement) {
//        this.preparedStatement = preparedStatement;
//    }
//
//    @Override
//    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//        String methodName = method.getName();
//        boolean isExecute = isExecuteQuery(methodName);
//
//        if (isExecute) {
//            String rawSql = preparedStatement.toString();
//            String fullSql = SqlUtils.extractSqlFromPreparedStatement(rawSql);
//            DynamicLogFileGenerator.log("Executing SQL: " + fullSql);  // ← 에이전트 로그 수집
//        }
//
//        return method.invoke(preparedStatement, args);
//    }
//
//    private boolean isExecuteQuery(String methodName) {
//        return methodName.equals("execute") || methodName.equals("executeQuery") || methodName.equals("executeUpdate");
//    }
//}
