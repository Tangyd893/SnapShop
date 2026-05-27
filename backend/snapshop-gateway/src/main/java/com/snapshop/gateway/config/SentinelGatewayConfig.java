package com.snapshop.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.snapshop.gateway.handler.SentinelFallbackHandler;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Sentinel 网关配置
 */
@Configuration
public class SentinelGatewayConfig {

    private final SentinelFallbackHandler fallbackHandler;

    public SentinelGatewayConfig(SentinelFallbackHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    @PostConstruct
    public void init() {
        // 设置网关限流回调
        GatewayCallbackManager.setBlockHandler((exchange, t) -> {
            return fallbackHandler.handleFallback(exchange, "请求过于频繁，请稍后重试");
        });
    }
}
