package com.example.tracing.sockettracing;

import com.example.tracing.logging.DynamicLogFileGenerator;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;
import java.util.Arrays;

public class SocketInterceptor {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method,
                               @Advice.AllArguments Object[] args) {
        Object endpoint = args[0];
        String address = endpoint.toString();
        System.out.println("[Agent] " + method.getName() + "() 호출됨");
        String ip = "";
        int slash = address.indexOf('/');
        int colon = address.indexOf(':');
        if (slash >= 0 && colon > slash) {
            ip = address.substring(slash + 1, colon);
        }
        System.out.println("[Agent] " + ip + "ip 호출됨");

        String[] interceptIpArray = {
                "142.250.198.110",
                "172.30.10.48",
                "172.30.12.30",
                "172.23.29.11"
        };

        if (Arrays.asList(interceptIpArray).contains(ip)) {
            DynamicLogFileGenerator.log("Socket : " + address);
        }
    }
}