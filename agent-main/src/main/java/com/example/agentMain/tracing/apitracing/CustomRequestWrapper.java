package com.example.agentMain.tracing.apitracing;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CustomRequestWrapper extends HttpServletRequestWrapper {

    private final Charset encoding;
    private final byte[] rawData;

    public CustomRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        // 요청 인코딩 설정 확인 (없으면 기본값 UTF-8 사용)
        String characterEncoding = request.getCharacterEncoding();
        if (StringUtils.isBlank(characterEncoding)) {
            characterEncoding = StandardCharsets.UTF_8.name();
        }
        this.encoding = Charset.forName(characterEncoding);

        // 본문 읽기 및 null 방지 처리
        byte[] requestBody = null;
        try (InputStream inputStream = request.getInputStream()) {
            if (inputStream != null) {
                requestBody = IOUtils.toByteArray(inputStream);
            }
        } catch (Exception e) {
            requestBody = new byte[0]; // 예외 발생 시 비어 있는 본문으로 처리
        }

        this.rawData = (requestBody != null) ? requestBody : new byte[0];
    }

    public byte[] getRawData() {
        return rawData;
    }

    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                this.rawData != null ? this.rawData : new byte[0]
        );

        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 비동기 처리 없음
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream(), this.encoding));
    }
}