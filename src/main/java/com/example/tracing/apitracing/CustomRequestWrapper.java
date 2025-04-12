package com.example.tracing.apitracing;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;

public class CustomRequestWrapper {

    private final Object request;  // javax.servlet.http.HttpServletRequest 인스턴스
    private final byte[] rawData;
    private final Charset encoding;

    public CustomRequestWrapper(Object request) {
        this.request = request;

        String characterEncoding = invokeStringMethod("getCharacterEncoding");
        if (characterEncoding == null || characterEncoding.isEmpty()) {
            characterEncoding = StandardCharsets.UTF_8.name();
        }
        this.encoding = Charset.forName(characterEncoding);

        this.rawData = readRequestBody();
    }

    /**
     * 요청 바디를 byte[]로 읽어 저장
     */
    private byte[] readRequestBody() {
        try {
            Method getInputStream = request.getClass().getMethod("getInputStream");
            InputStream inputStream = (InputStream) getInputStream.invoke(request);
            return IOUtils.toByteArray(inputStream);
        } catch (Exception e) {
            System.err.println("[Agent] ❌ RequestBody 읽기 실패: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * InputStream 반환 (rawData 기반)
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(rawData);
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
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), encoding));
    }

    /**
     * 요청 URL 추출
     */
    public String getRequestURL() {
        try {
            StringBuilder url = new StringBuilder();
            String scheme = invokeStringMethod("getScheme");
            String serverName = invokeStringMethod("getServerName");
            int serverPort = invokeIntMethod("getServerPort");
            String contextPath = invokeStringMethod("getContextPath");
            String servletPath = invokeStringMethod("getServletPath");
            String pathInfo = invokeStringMethod("getPathInfo");
            String queryString = invokeStringMethod("getQueryString");

            url.append(scheme).append("://").append(serverName);
            if (serverPort != 80 && serverPort != 443) {
                url.append(":").append(serverPort);
            }
            url.append(contextPath).append(servletPath);
            if (pathInfo != null) {
                url.append(pathInfo);
            }
            if (queryString != null) {
                url.append("?").append(queryString);
            }
            return url.toString();
        } catch (Exception e) {
            System.err.println("[Agent] ❌ RequestURL 생성 실패: " + e.getMessage());
            return "";
        }
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

    public byte[] getRawData() {
        return rawData;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public Object getOriginalRequest() {
        return request;
    }
}
