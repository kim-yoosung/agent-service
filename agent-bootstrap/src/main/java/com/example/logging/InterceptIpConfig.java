package com.example.logging;

import java.util.Arrays;
import java.util.List;

public class InterceptIpConfig {

    private static final List<String> interceptIpList = Arrays.asList(
            "172.30.10.48",
            "172.30.12.30",
            "172.23.15.36",
            "172.20.32.104",
            "172.20.32.105",
            "172.23.29.11",
            "4.68.0.39",
            "172.17.26.137",
            "211.115.124.38",
            "142.250.197.78"
    );

    public static boolean shouldIntercept(String ip) {
        return interceptIpList.contains(ip);
    }
}
