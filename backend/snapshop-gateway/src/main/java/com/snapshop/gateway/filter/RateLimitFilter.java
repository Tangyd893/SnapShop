package com.snapshop.gateway.filter;

import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 基础限流过滤器 - 基于 Redis 滑动窗口
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String KEY_PREFIX = "ratelimit:";
    private static final String SECKILL_PATH = "/api/seckill/";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 仅对秒杀相关写路径限流
        if (!path.contains(SECKILL_PATH) || path.endsWith("/token")) {
            return chain.filter(exchange);
        }

        String clientIp = getClientIp(request);
        String limitKey = KEY_PREFIX + "seckill:" + clientIp;

        return redisTemplate.opsForValue()
                .increment(limitKey)
                .flatMap(count -> {
                    if (count == 1) {
                        redisTemplate.expire(limitKey, Duration.ofSeconds(60)).subscribe();
                    }
                    if (count > 100) {
                        return writeRateLimited(exchange, clientIp, count);
                    }
                    return chain.filter(exchange);
                });
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> writeRateLimited(ServerWebExchange exchange, String ip, long count) {
        log.warn("限流触发: IP={}, 请求数={}", ip, count);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        R<Void> r = R.fail(ErrorCode.RATE_LIMITED.getCode(), "请求过于频繁，请稍后重试");
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(r);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
