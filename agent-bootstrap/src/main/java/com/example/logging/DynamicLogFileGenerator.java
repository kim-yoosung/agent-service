package com.example.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DynamicLogFileGenerator {

    private static final ThreadLocal<BufferedWriter> writerHolder = new ThreadLocal<>();

    public static void initLogger() {
        try {
            String fileName = "logs/agent_" + System.currentTimeMillis() + ".log";
            File logFile = new File(fileName);
            logFile.getParentFile().mkdirs();

            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            writerHolder.set(writer);
            System.out.println("[Agent] Incoming Request/Response Filter Initialized." + fileName);
            DynamicLogFileGenerator.log("Incoming Request/Response Filter Initialized. " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        try {
            BufferedWriter writer = writerHolder.get();
            if (writer != null) {
                writer.write("[Agent] " + message + "\n");
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void finishLogger() {
        try {
            BufferedWriter writer = writerHolder.get();
            System.out.println("[Agent] Incoming Request/Response Filter finishLogger.");
            DynamicLogFileGenerator.log("Incoming Request/Response Filter finishLogger.");
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writerHolder.remove();
        }
    }
}
