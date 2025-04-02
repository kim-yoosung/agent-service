package com.example.tracing.outgingtracing;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.*;

public class ClientHttpResponseWrapper implements ClientHttpResponse {

    private final ClientHttpResponse original;
    private byte[] bodyBytes;

    public ClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
        this.original = response;

        // Body 스트림을 byte[]로 복사
        InputStream bodyStream = response.getBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;

        while ((nRead = bodyStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        this.bodyBytes = buffer.toByteArray();
    }

    @Override
    public InputStream getBody() {
        // 복사된 바이트 배열을 새 InputStream으로 다시 제공
        return new ByteArrayInputStream(bodyBytes);
    }

    @Override
    public HttpHeaders getHeaders() {
        return original.getHeaders();
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        return original.getStatusCode();
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return original.getRawStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return original.getStatusText();
    }

    @Override
    public void close() {
        original.close();
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }
}
