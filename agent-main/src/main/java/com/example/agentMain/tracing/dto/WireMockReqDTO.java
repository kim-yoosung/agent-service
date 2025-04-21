package com.example.agentMain.tracing.dto;

import java.util.List;
import java.util.Map;

public class WireMockReqDTO {
    private String method;
    private String url;
    private String urlPattern;
    private Map<String, String> headers;
    private List<Map<String, String>> body;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public List<Map<String, String>> getBody() {
        return body;
    }

    public void setBody(List<Map<String, String>> body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "WireMockReqDTO{" +
                "method='" + method + '\'' +
                ", uri='" + url + '\'' +
                ", uriPattern='" + urlPattern + '\'' +
                ", headers=" + headers +
                ", body=" + body +
                '}';
    }
}
