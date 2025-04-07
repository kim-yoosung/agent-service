//package com.example.tracing.outgingtracing;
//
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.client.ClientHttpResponse;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//public class AgentEmptyResponseProvider {
//
//    public static ClientHttpResponse create() {
//        return new ClientHttpResponse() {
//
//            @Override
//            public HttpStatus getStatusCode() throws IOException {
//                return HttpStatus.OK;
//            }
//
//            @Override
//            public int getRawStatusCode() throws IOException {
//                return HttpStatus.OK.value();
//            }
//
//            @Override
//            public String getStatusText() throws IOException {
//                return HttpStatus.OK.getReasonPhrase();
//            }
//
//            @Override
//            public void close() {
//                // nothing to close
//            }
//
//            @Override
//            public InputStream getBody() throws IOException {
//                return new ByteArrayInputStream(new byte[0]); // 빈 바디
//            }
//
//            @Override
//            public HttpHeaders getHeaders() {
//                return new HttpHeaders(); // 빈 헤더
//            }
//        };
//    }
//}
