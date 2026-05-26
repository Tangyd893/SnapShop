package com.snapshop.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 公共模块自动配置
 */
@Configuration
@ComponentScan(basePackages = "com.snapshop.common")
public class CommonAutoConfiguration {
}
