package com.example.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicLogFileGenerator {
    private static final Map<String, String> transactionToFile = new ConcurrentHashMap<>();
    private static final Map<String, BufferedWriter> fileWriters = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentTxId = new ThreadLocal<>();

    public static void initLogger() {
        try {
            String txId = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            String fileName = "logs/agent_" + System.currentTimeMillis() + ".log";
            File logFile = new File(fileName);
            logFile.getParentFile().mkdirs();
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            
            transactionToFile.put(txId, fileName);
            fileWriters.put(fileName, writer);
            currentTxId.set(txId);
            
            System.out.println("[Agent] Incoming Request/Response Filter Initialized." + fileName);
            log("Incoming Request/Response Filter Initialized. " + fileName);
            System.out.println("[Agent] current thread in DispatcherServletAdvice: " + Thread.currentThread().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentTransactionId() {
        return currentTxId.get();
    }

    public static void setCurrentTransaction(String txId) {
        if (txId != null) {
            currentTxId.set(txId);
        }
    }

    public static void log(String message) {
        String txId = currentTxId.get();
        if (txId == null) {
            return;
        }

        String fileName = transactionToFile.get(txId);
        if (fileName == null) {
            return;
        }

        BufferedWriter writer = fileWriters.get(fileName);
        if (writer == null) {
            return;
        }

        try {
            writer.write("[Agent] " + message + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void finishLogger() {
        String txId = currentTxId.get();
        if (txId != null) {
            String fileName = transactionToFile.get(txId);
            if (fileName != null) {
                BufferedWriter writer = fileWriters.get(fileName);
                if (writer != null) {
                    try {
                        System.out.println("[Agent] Incoming Request/Response Filter finishLogger.");
                        log("Incoming Request/Response Filter finishLogger.");
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        fileWriters.remove(fileName);
                        transactionToFile.remove(txId);
                        currentTxId.remove();
                    }
                }
            }
        }
    }
}
