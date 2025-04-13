package com.example.tracing.apitracing;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CustomResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream copyStream;
    private final ServletOutputStream outputStream;
    private final HttpServletResponse response;
    private PrintWriter writer;

    public CustomResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
        this.copyStream = new ByteArrayOutputStream();
        this.outputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public void write(int b) throws IOException {
                copyStream.write(b);
                response.getOutputStream().write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                copyStream.write(b);
                response.getOutputStream().write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                copyStream.write(b, off, len);
                response.getOutputStream().write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                copyStream.flush();
                response.getOutputStream().flush();
            }

            @Override
            public void close() throws IOException {
                copyStream.close();
                response.getOutputStream().close();
            }
        };
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(outputStream, getCharacterEncoding()));
        }
        return writer;
    }

    public byte[] getBody() {
        return copyStream.toByteArray();
    }

    public String getBodyAsString() {
        return new String(getBody(), getCharset());
    }

    /**
     * 인코딩 정보 가져오기 (없으면 UTF-8)
     */
    private Charset getCharset() {
        try {
            Method m = response.getClass().getMethod("getCharacterEncoding");
            String encoding = (String) m.invoke(response);
            return encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    public byte[] getCapturedBodyAsBytes() {
        return copyStream.toByteArray();
    }

    public String getCapturedBody() {
        return new String(copyStream.toByteArray(), getCharset());
    }

    public Object getOriginalResponse() {
        return response;
    }

    public int getStatus() {
        try {
            Method m = response.getClass().getMethod("getStatus");
            return (int) m.invoke(response);
        } catch (Exception e) {
            System.err.println("[Agent] ⚠️ getStatus() 호출 실패: " + e.getMessage());
            return -1;
        }
    }
}
