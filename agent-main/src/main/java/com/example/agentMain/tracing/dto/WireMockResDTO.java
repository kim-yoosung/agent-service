package com.example.agentMain.tracing.dto;

import java.util.Map;

public class WireMockResDTO {
    private Integer status;
    private String body;
    private Map<String, String> headers;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "WireMockResDTO{" +
                "status=" + status +
                ", body='" + body + '\'' +
                ", headers=" + headers +
                '}';
    }
}
