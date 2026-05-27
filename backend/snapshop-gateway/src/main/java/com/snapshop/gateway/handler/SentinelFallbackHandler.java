package com.snapshop.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Sentinel 限流降级处理器
 */
@Slf4j
@Component
public class SentinelFallbackHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 处理限流降级响应
     */
    public Mono<ServerResponse> handleFallback(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> r = R.fail(ErrorCode.RATE_LIMITED.getCode(), message);
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(r);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer))
                    .then(Mono.empty());
        } catch (Exception e) {
            log.error("序列化降级响应失败", e);
            return response.setComplete().then(Mono.empty());
        }
    }
}