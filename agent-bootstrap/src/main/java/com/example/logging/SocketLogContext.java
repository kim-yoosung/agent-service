package com.example.logging;

public class SocketLogContext {
    private static final ThreadLocal<String> fileNameHolder = new ThreadLocal<>();

    public static void setFileName(String fileName) {
        fileNameHolder.set(fileName);
    }

    public static String getFileName() {
        return fileNameHolder.get();
    }

    public static void clear() {
        fileNameHolder.remove();
    }
}
