package com.example.tracing.apitracing;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CustomRequestWrapper2 extends HttpServletRequestWrapper {

    private final Charset encoding;
    private final byte[] rawData;

    public CustomRequestWrapper2(HttpServletRequest request) throws IOException {
        super(request);

        // 요청 인코딩 설정 확인 (없으면 기본값 UTF-8 사용)
        String characterEncoding = request.getCharacterEncoding();
        if (StringUtils.isBlank(characterEncoding)) {
            characterEncoding = StandardCharsets.UTF_8.name();
        }
        this.encoding = Charset.forName(characterEncoding);

        try (InputStream inputStream = request.getInputStream()) {
            this.rawData = IOUtils.toByteArray(inputStream);
        }
    }

    public byte[] getRawData() {
        return rawData;
    }

    public Charset getEncoding() {
        return encoding;
    }

    // 4. getInputStream() 오버라이드하여 저장된 데이터를 다시 읽을 수 있도록 함
    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.rawData);

        ServletInputStream servletInputStream = new ServletInputStream(){
            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {}
        };

        return servletInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream(), this.encoding));
    }
}
