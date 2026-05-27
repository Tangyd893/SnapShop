package com.snapshop.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;

/**
 * Sentinel 网关配置
 */
@Slf4j
@Configuration
public class SentinelGatewayConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostConstruct
    public void init() {
        // 设置网关限流回调，返回 Mono<ServerResponse>
        GatewayCallbackManager.setBlockHandler((exchange, ex) -> {
            log.warn("Sentinel 网关限流触发: {}", ex.getMessage());
            return writeFallbackResponse(exchange);
        });
    }

    /**
     * 写入限流降级响应
     */
    private Mono<ServerResponse> writeFallbackResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> r = R.fail(ErrorCode.RATE_LIMITED.getCode(), "请求过于频繁，请稍后重试");
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
