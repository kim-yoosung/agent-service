package com.example.tracing.apitracing;

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
