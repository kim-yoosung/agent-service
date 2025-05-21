package com.example.logging;

public class SocketConnectionContext {
    private static final ThreadLocal<String> currentIp = new ThreadLocal<>();
    private static final ThreadLocal<String> currentPort = new ThreadLocal<>();


    public static void setCurrentIp(String ip) {
        currentIp.set(ip);
    }

    public static void setCurrentPort(String port) {
        currentPort.set(port);
    }

    public static String getCurrentIp() {
        return currentIp.get();
    }

    public static String getCurrentPort() {
        return currentPort.get();
    }

    public static void clear() {
        currentIp.remove();
    }
}
