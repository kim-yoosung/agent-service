package com.example.agentMain.tracing.sockettracing;

import com.example.logging.InterceptIpConfig;
import com.example.logging.LoggingInputStream;
import com.example.logging.SocketConnectionContext;
import net.bytebuddy.asm.Advice;

import java.io.InputStream;
import java.net.Socket;


public class GetInputStreamAdvice {

    @Advice.OnMethodExit
    public static void onExit(@Advice.Return(readOnly = false) InputStream returned,
                              @Advice.This Socket socket) {

        String ip = SocketConnectionContext.getCurrentIp();
        if (InterceptIpConfig.shouldIntercept(ip)) {
            System.out.println("[Agent] getInputStream() 후킹됨 - IP: " + ip);
            returned = new LoggingInputStream(returned);
        }

    }
}