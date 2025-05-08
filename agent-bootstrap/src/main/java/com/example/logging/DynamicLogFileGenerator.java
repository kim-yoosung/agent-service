package com.example.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicLogFileGenerator {
    private static final Map<Long, String> threadToTxId = new ConcurrentHashMap<>();
    private static final Map<String, String> transactionToFile = new ConcurrentHashMap<>();
    private static final Map<String, BufferedWriter> fileWriters = new ConcurrentHashMap<>();

    public static void initLogger() {
        try {
            long threadId = Thread.currentThread().getId();
            String txId = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            String fileName = "logs/agent_" + System.currentTimeMillis() + ".log";
            
            File logFile = new File(fileName);
            logFile.getParentFile().mkdirs();
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            
            threadToTxId.put(threadId, txId);
            transactionToFile.put(txId, fileName);
            fileWriters.put(fileName, writer);
            
            System.out.println("[Agent] Incoming Request/Response Filter Initialized. " + fileName);
            log("Incoming Request/Response Filter Initialized. " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentTransactionId() {
        long threadId = Thread.currentThread().getId();
        // 현재 스레드의 트랜잭션 ID가 없으면 부모 스레드에서 찾기
        if (!threadToTxId.containsKey(threadId)) {
            // 스레드 그룹의 모든 스레드를 확인
            ThreadGroup group = Thread.currentThread().getThreadGroup();
            Thread[] threads = new Thread[group.activeCount()];
            group.enumerate(threads);
            
            for (Thread thread : threads) {
                if (thread != null && threadToTxId.containsKey(thread.getId())) {
                    String parentTxId = threadToTxId.get(thread.getId());
                    threadToTxId.put(threadId, parentTxId);
                    return parentTxId;
                }
            }
        }
        return threadToTxId.get(threadId);
    }

    public static void setCurrentTransaction(String txId) {
        if (txId != null) {
            threadToTxId.put(Thread.currentThread().getId(), txId);
        }
    }

    public static void log(String message) {
        try {
            String txId = getCurrentTransactionId();
            if (txId != null) {
                String fileName = transactionToFile.get(txId);
                BufferedWriter writer = fileWriters.get(fileName);
                if (writer != null) {
                    writer.write("[Agent] " + message + "\n");
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void finishLogger() {
        try {
            long threadId = Thread.currentThread().getId();
            String txId = threadToTxId.remove(threadId);
            if (txId != null) {
                String fileName = transactionToFile.remove(txId);
                if (fileName != null) {
                    BufferedWriter writer = fileWriters.remove(fileName);
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
