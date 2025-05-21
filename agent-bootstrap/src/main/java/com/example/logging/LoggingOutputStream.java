package com.example.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

public class LoggingOutputStream extends OutputStream {
    private final OutputStream delegate;
    private final String ip;
    private final String port;


    public LoggingOutputStream(OutputStream delegate, String ip, String port) {
        this.delegate = delegate;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void write(int b) throws IOException {
        DynamicLogFileGenerator.log("[OUT byte] " + b);
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        String content = new String(b, off, len, "EUC-KR");
        writeLog("[Agent Socket] IP: " + ip + " Port: " + port + "\n");
        writeLog("[OUT byte] " + content);
        delegate.write(b, off, len);
    }

    private void writeLog(String content) {
        String filePath = SocketLogContext.getFileName();
        if (filePath != null) {
            try (FileWriter writer = new FileWriter(filePath, true)) {
                writer.write(content + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
