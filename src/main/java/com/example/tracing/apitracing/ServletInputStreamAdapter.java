//package com.example.tracing.apitracing;
//
//import javax.servlet.ReadListener;
//import javax.servlet.ServletInputStream;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//
//public class ServletInputStreamAdapter extends ServletInputStream {
//
//    private final ByteArrayInputStream inputStream;
//
//    public ServletInputStreamAdapter(ByteArrayInputStream inputStream) {
//        this.inputStream = inputStream;
//    }
//
//    @Override
//    public int read() throws IOException {
//        return inputStream.read();
//    }
//
//    @Override
//    public boolean isFinished() {
//        return inputStream.available() == 0;
//    }
//
//    @Override
//    public boolean isReady() {
//        return true;
//    }
//
//    @Override
//    public void setReadListener(ReadListener readListener) {
//        // 비동기 Servlet 환경이 아닌 경우 생략 가능
//    }
//}
