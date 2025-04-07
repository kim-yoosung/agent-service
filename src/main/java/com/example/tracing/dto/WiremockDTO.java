package com.example.tracing.dto;

public class WiremockDTO {

    private WireMockReqDTO request;
    private WireMockResDTO response;

    public WireMockReqDTO getRequest() {
        return request;
    }

    public void setRequest(WireMockReqDTO request) {
        this.request = request;
    }

    public WireMockResDTO getResponse() {
        return response;
    }

    public void setResponse(WireMockResDTO response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "WiremockDTO{" +
                "request=" + request +
                ", response=" + response +
                '}';
    }
}
