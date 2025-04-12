package com.example.tracing.apitracing;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CustomResponseWrapper {

    private final Object originalResponse;
    private final ByteArrayOutputStream copy = new ByteArrayOutputStream();
    private ServletOutputStream wrappedOutputStream;
    private PrintWriter writer;

    public CustomResponseWrapper(Object response) {
        this.originalResponse = response;
    }

    /**
     * wrapped ServletOutputStream
     */
    public ServletOutputStream getOutputStream() {
        if (wrappedOutputStream == null) {
            wrappedOutputStream = new ServletOutputStream() {

                private final ServletOutputStream originalStream = getOriginalOutputStream();

                @Override
                public void write(int b) throws IOException {
                    originalStream.write(b);
                    copy.write(b);
                }

                @Override
                public void flush() throws IOException {
                    originalStream.flush();
                    copy.flush();
                }

                @Override
                public void close() throws IOException {
                    originalStream.close();
                    copy.close();
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener listener) {
                    // 비동기 사용 시 구현 필요
                }
            };
        }
        return wrappedOutputStream;
    }

    /**
     * getWriter (기본 인코딩은 UTF-8)
     */
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getEncoding()), true);
        }
        return writer;
    }

    public byte[] toByteArray() {
        return copy.toByteArray();
    }

    /**
     * 원본 응답에서 getOutputStream 리플렉션 호출
     */
    private ServletOutputStream getOriginalOutputStream() {
        try {
            Method method = originalResponse.getClass().getMethod("getOutputStream");
            return (ServletOutputStream) method.invoke(originalResponse);
        } catch (Exception e) {
            throw new RuntimeException("[Agent] ❌ 원본 OutputStream 추출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 인코딩 정보 가져오기 (없으면 UTF-8)
     */
    private Charset getEncoding() {
        try {
            Method m = originalResponse.getClass().getMethod("getCharacterEncoding");
            String encoding = (String) m.invoke(originalResponse);
            return encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    public byte[] getCapturedBodyAsBytes() {
        return copy.toByteArray();
    }

    public String getCapturedBody() {
        return new String(copy.toByteArray(), getEncoding());
    }

    public Object getOriginalResponse() {
        return originalResponse;
    }

    public int getStatus() {
        try {
            Method m = originalResponse.getClass().getMethod("getStatus");
            return (int) m.invoke(originalResponse);
        } catch (Exception e) {
            System.err.println("[Agent] ⚠️ getStatus() 호출 실패: " + e.getMessage());
            return -1;
        }
    }
}
