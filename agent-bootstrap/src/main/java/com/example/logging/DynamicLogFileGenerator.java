package com.example.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DynamicLogFileGenerator {

    private static BufferedWriter writer;

    public static void initLogger() {
        try {
            String fileName = "logs/agent_" + System.currentTimeMillis() + ".log";
            File logFile = new File(fileName);
            logFile.getParentFile().mkdirs();
            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write("[Agent] " + fileName + " generated.");
            System.out.println("[Agent] " + fileName + " generated.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        try {
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
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
