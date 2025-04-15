package com.example.tracing.apitracing;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.output.TeeOutputStream;

public class CustomResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream copyStream = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private boolean usingWriter = false;
    private boolean usingStream = false;

    public CustomResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (usingWriter) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }
        usingStream = true;

        if (outputStream == null) {
            System.out.println("[Agent Debug] Creating custom ServletOutputStream for getOutputStream...");
            final ServletOutputStream originalOutputStream = super.getOutputStream();
            outputStream = new ServletOutputStream() {
                private final TeeOutputStream tee = new TeeOutputStream(originalOutputStream, copyStream);

                @Override
                public void write(int b) throws IOException {
                    tee.write(b);
                }
                @Override
                public void write(byte[] b) throws IOException {
                    tee.write(b);
                }
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    tee.write(b, off, len);
                }
                @Override
                public void flush() throws IOException {
                    tee.flush();
                }
                @Override
                public void close() throws IOException {
                    System.out.println("[Agent Debug] Custom ServletOutputStream closed. Captured size: " + copyStream.size());
                    tee.close();
                }
                @Override
                public boolean isReady() {
                    return originalOutputStream.isReady();
                }
                @Override
                public void setWriteListener(WriteListener listener) {
                    originalOutputStream.setWriteListener(listener);
                }
            };
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (usingStream) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }
        usingWriter = true;

        if (writer == null) {
            OutputStream originalStream = super.getOutputStream();
            TeeOutputStream teeStream = new TeeOutputStream(originalStream, copyStream);
            writer = new PrintWriter(new OutputStreamWriter(teeStream, getCharacterEncodingOrDefault()), true);
        }
        return writer;
    }

    public byte[] getCapturedBodyAsBytes() {
        return copyStream.toByteArray();
    }
    
    private String getCharacterEncodingOrDefault() {
        String encoding = super.getCharacterEncoding();
        return encoding != null ? encoding : StandardCharsets.UTF_8.name();
    }
    
    private Charset getCharset() {
        String encoding = getCharacterEncodingOrDefault();
        return Charset.forName(encoding);
    }
}
