package com.example.agent;

public class AgentConfig {

    public static String getOwnJarPath() {
        return AgentMain.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
    }
}
