package com.example.logging;


import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class LoggingInputStream extends InputStream {
    private final InputStream delegate;

    public LoggingInputStream(InputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        int b = delegate.read();
        if (b != -1) {
            DynamicLogFileGenerator.log("[IN byte] " + b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int count = delegate.read(b, off, len);
        if (count > 0) {
            String content = new String(b, off, count, "UTF-8");
            writeLog("[IN] " + content);
        }
        return count;
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
    public void close() throws IOException {
        delegate.close();
    }
}