package com.example.agentMain.tracing.sockettracing;

import com.example.logging.DynamicLogFileGenerator;
import com.example.logging.InterceptIpConfig;
import com.example.logging.SocketConnectionContext;
import com.example.logging.SocketLogContext;
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
        String port = "";
        int slash = address.indexOf('/');
        int colon = address.indexOf(':');
        if (slash >= 0 && colon > slash) {
            ip = address.substring(slash + 1, colon);
            port = address.substring(colon + 1);
        }
        System.out.println("[Agent - socket] " + ip + ":" + port);

        if (InterceptIpConfig.shouldIntercept(ip)) {
            SocketConnectionContext.setCurrentIp(ip);
            String fileName = "logs/" + "socket-" + System.currentTimeMillis() + ".txt";
            SocketLogContext.setFileName(fileName); // 스레드별 고유 파일명 등록

            System.out.println("[Agent Socket] " + ip + " 원하는 소켓 ip 호출됨!!!");
            DynamicLogFileGenerator.log("Socket: " + fileName);
        }
    }
}