package com.example.logging;

public class SocketConnectionContext {
    private static final ThreadLocal<String> currentIp = new ThreadLocal<>();

    public static void setCurrentIp(String ip) {
        currentIp.set(ip);
    }

    public static String getCurrentIp() {
        return currentIp.get();
    }

    public static void clear() {
        currentIp.remove();
    }
}
