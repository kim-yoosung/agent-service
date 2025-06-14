package com.example.agentMain.tracing.dto;

import java.util.List;
import java.util.Map;

public class WireMockReqDTO {
    private String method;
    private String url;
    private String urlPattern;
    private Map<String, String> headers;
    private List<Map<String, String>> bodyPatterns;

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

    public List<Map<String, String>> getBodyPatterns() {
        return bodyPatterns;
    }

    public void setBodyPatterns(List<Map<String, String>> bodyPatterns) {
        this.bodyPatterns = bodyPatterns;
    }

    @Override
    public String toString() {
        return "WireMockReqDTO{" +
                "method='" + method + '\'' +
                ", uri='" + url + '\'' +
                ", uriPattern='" + urlPattern + '\'' +
                ", headers=" + headers +
                ", body=" + bodyPatterns +
                '}';
    }
}
