package com.example.tracing.apitracing;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class CustomRequestWrapper extends HttpServletRequestWrapper {

    private final Object request;  // javax.servlet.http.HttpServletRequest 인스턴스
    private final Charset encoding;
    private final ByteArrayOutputStream cachedBytes;
    private final ServletInputStream inputStream;
    private BufferedReader reader;

    public CustomRequestWrapper(HttpServletRequest request) {
        super(request);
        this.request = request;

        String characterEncoding = invokeStringMethod("getCharacterEncoding");
        if (characterEncoding == null || characterEncoding.isEmpty()) {
            characterEncoding = StandardCharsets.UTF_8.name();
        }
        this.encoding = Charset.forName(characterEncoding);

        this.cachedBytes = new ByteArrayOutputStream();
        this.inputStream = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public int read() throws IOException {
                int data = request.getInputStream().read();
                if (data != -1) {
                    cachedBytes.write(data);
                }
                return data;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int count = request.getInputStream().read(b, off, len);
                if (count != -1) {
                    cachedBytes.write(b, off, count);
                }
                return count;
            }
        };
    }

    /**
     * InputStream 반환 (rawData 기반)
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }

    /**
     * ServletInputStream 스타일로 반환 (Filter나 Interceptor에서 사용 가능하게끔)
     */
//    public Object getServletInputStream() {
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(rawData);
//
//        return new ServletInputStreamAdapter(byteArrayInputStream);
//    }

    /**
     * BufferedReader 반환
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
        }
        return reader;
    }

    /**
     * 요청 URL 추출
     */
    @Override
    public StringBuffer getRequestURL() {
        return super.getRequestURL();
    }

    /**
     * HTTP Method 추출
     */
    public String getMethod() {
        return invokeStringMethod("getMethod");
    }

    public String getQueryString() {
        return invokeStringMethod("getQueryString");
    }

    /**
     * Header 추출
     */
    public String getHeader(String name) {
        try {
            Method method = request.getClass().getMethod("getHeader", String.class);
            return (String) method.invoke(request, name);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Enumeration<String> getHeaderNames() {
        try {
            Method m = request.getClass().getMethod("getHeaderNames");
            return (Enumeration<String>) m.invoke(request);
        } catch (Exception e) {
            return Collections.emptyEnumeration();
        }
    }

    private String invokeStringMethod(String methodName) {
        try {
            Method m = request.getClass().getMethod(methodName);
            return (String) m.invoke(request);
        } catch (Exception e) {
            return null;
        }
    }

    private int invokeIntMethod(String methodName) {
        try {
            Method method = request.getClass().getMethod(methodName);
            return (int) method.invoke(request);
        } catch (Exception e) {
            return 0;
        }
    }

    public Charset getEncoding() {
        return encoding;
    }

    public Object getOriginalRequest() {
        return request;
    }

    public byte[] getBody() {
        return cachedBytes.toByteArray();
    }

    public String getBodyAsString() {
        try {
            return new String(getBody(), getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            return new String(getBody());
        }
    }
}
