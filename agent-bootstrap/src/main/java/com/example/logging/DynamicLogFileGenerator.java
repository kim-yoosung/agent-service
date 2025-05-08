package com.example.logging;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicLogFileGenerator {
    private static final ConcurrentHashMap<Long, String> threadToTransactionMap = new ConcurrentHashMap<>();
    private static final String LOG_FILE_PREFIX = "dynamiclog";
    private static final String LOG_FILE_EXTENSION = ".log";
    
    public static void initLogger(String transactionId) {
        if (transactionId != null) {
            threadToTransactionMap.put(Thread.currentThread().getId(), transactionId);
        }
    }

    public static String getCurrentTransactionId() {
        String transactionId = threadToTransactionMap.get(Thread.currentThread().getId());
        
        if (transactionId == null) {
            // 현재 스레드의 트랜잭션 ID가 없으면 부모 스레드에서 찾기
            ThreadGroup group = Thread.currentThread().getThreadGroup();
            Thread[] threads = new Thread[group.activeCount()];
            group.enumerate(threads);
            
            for (Thread thread : threads) {
                if (thread != null) {
                    String parentTxId = threadToTransactionMap.get(thread.getId());
                    if (parentTxId != null) {
                        // 부모 스레드의 트랜잭션 ID를 현재 스레드에도 설정
                        threadToTransactionMap.put(Thread.currentThread().getId(), parentTxId);
                        System.out.println("[Agent] Inherited Transaction ID: " + parentTxId + " from thread: " + thread.getId());
                        return parentTxId;
                    }
                }
            }
        }
        
        return transactionId;
    }

    public static void setCurrentTransaction(String transactionId) {
        if (transactionId != null) {
            threadToTransactionMap.put(Thread.currentThread().getId(), transactionId);
        }
    }

    public static void log(String message) {
        String transactionId = getCurrentTransactionId();
        if (transactionId != null) {
            String fileName = LOG_FILE_PREFIX + "_" + transactionId + LOG_FILE_EXTENSION;
            try (FileWriter fw = new FileWriter(fileName, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(message);
            } catch (IOException e) {
                System.err.println("Error writing to log file: " + e.getMessage());
            }
        }
    }

    public static void finishLogger() {
        threadToTransactionMap.remove(Thread.currentThread().getId());
    }
}
