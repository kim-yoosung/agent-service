package com.example.agentMain.tracing.sockettracing;

import com.example.logging.InterceptIpConfig;
import com.example.logging.LoggingOutputStream;
import com.example.logging.SocketConnectionContext;
import net.bytebuddy.asm.Advice;

import java.io.OutputStream;
import java.net.Socket;

public class GetOutputStreamAdvice {

    @Advice.OnMethodExit
    public static void onExit(@Advice.Return(readOnly = false) OutputStream returned) {

        String ip = SocketConnectionContext.getCurrentIp();
        if (ip != null && InterceptIpConfig.shouldIntercept(ip)) {
            System.out.println("[Agent] getOutputStream() 후킹됨 - IP: " + ip);
            returned = new LoggingOutputStream(returned);
        }
    }
}