package com.example.tracing.outgingtracing;

import net.bytebuddy.implementation.bind.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import java.net.URI;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.example.tracing.outgingtracing.OutgingUtils.filterInactiveUrl;

public class RestTemplateInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RestTemplateInterceptor.class);

    @RuntimeType
    public static Object intercept(@SuperCall Callable<Object> zuper,
                                 @AllArguments Object[] args) throws Exception {
        try {
            Object uri = args[0];
            Object httpMethod = args[2];
            Object requestEntity = args[3];

            String uriStr = filterInactiveUrl(uri.toString());
            logger.info("[Agent] Outgoing Request - URI: {}, Method: {}", uriStr, httpMethod);

            String serviceName = OutgingUtils.extractServiceName(uriStr);
            logger.info("[Agent] Service Name: {}", serviceName);

            if (OutgingUtils.shouldSkipRequest(httpMethod.toString(), uriStr, serviceName)) {
                logger.info("[Agent] Skipping request due to filter conditions");
                return zuper.call();
            }

            // 요청 바디 로깅
            if (requestEntity instanceof HttpEntity) {
                HttpEntity<?> entity = (HttpEntity<?>) requestEntity;
                if (entity.getBody() != null) {
                    logger.info("[Agent] Request Body: {}", entity.getBody());
                }
            }

            // 원래 요청 실행
            Object responseObj = zuper.call();

            // 응답 처리
            if (responseObj instanceof ClientHttpResponse) {
                ClientHttpResponse original = (ClientHttpResponse) responseObj;
                
                // 응답 바디를 한 번만 읽고 캐시
                byte[] responseBody = StreamUtils.copyToByteArray(original.getBody());
                
                // 원래 응답을 래핑하여 바디를 다시 읽을 수 있도록 함
                ClientHttpResponseWrapper wrappedResponse = new ClientHttpResponseWrapper(original, responseBody);
                OutgingUtils.handleWiremockLogging(args, wrappedResponse);
                
                // 원래 응답을 복원하여 반환
                return new RestoredClientHttpResponse(original, responseBody);
            }

            return responseObj;

        } catch (Exception e) {
            logger.error("[Agent] Error in RestTemplateInterceptor", e);
            throw e;
        }
    }
}