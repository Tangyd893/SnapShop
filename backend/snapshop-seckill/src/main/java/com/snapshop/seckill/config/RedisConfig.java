package com.snapshop.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Redis 配置，加载 Lua 脚本
 */
@Configuration
public class RedisConfig {

    /**
     * 预加载秒杀库存预扣 Lua 脚本
     */
    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
