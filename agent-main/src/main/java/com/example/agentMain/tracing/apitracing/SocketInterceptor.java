package com.example.agentMain.tracing.apitracing;

import com.example.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;
import java.util.Arrays;

public class SocketInterceptor {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method,
                               @Advice.AllArguments Object[] args) {
        Object endpoint = args[0];
        String address = endpoint.toString();
        String ip = "";
        int slash = address.indexOf('/');
        int colon = address.indexOf(':');
        if (slash >= 0 && colon > slash) {
            ip = address.substring(slash + 1, colon);
        }
        System.out.println("[Agent - socket] " + ip);

        String[] interceptIpArray = {
                "172.30.10.48",
                "172.30.12.30",
                "172.23.15.36",
                "172.20.32.104",
                "172.20.32.105",
                "172.23.29.11",
                "4.68.0.39",
                "172.17.26.137",
                "211.115.124.38"
        };

        if (Arrays.asList(interceptIpArray).contains(ip)) {
            System.out.println("[Agent Socket] " + ip + " 원하는 소켓 ip 호출됨!!!");
            DynamicLogFileGenerator.log("Socket: " + address);
        }
    }
}