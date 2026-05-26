package com.snapshop.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.snapshop")
@MapperScan("com.snapshop.admin.mapper")
@EnableDiscoveryClient
@EnableFeignClients
public class SnapshopAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(SnapshopAdminApplication.class, args);
    }
}
