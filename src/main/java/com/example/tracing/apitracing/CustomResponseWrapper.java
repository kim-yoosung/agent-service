package com.example.tracing.apitracing;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.*;

public class CustomResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    public CustomResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }
        if (outputStream == null) {
            outputStream = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    byteArrayOutputStream.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    throw new UnsupportedOperationException();
                }
            };
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }
        if (writer == null) {
            writer = new PrintWriter(byteArrayOutputStream);
        }
        return writer;
    }

    public byte[] toByteArray() {
        return byteArrayOutputStream.toByteArray();
    }
}
