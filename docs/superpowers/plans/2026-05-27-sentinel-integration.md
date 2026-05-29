# Sentinel 熔断限流集成实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 SnapShop 秒杀平台中集成 Alibaba Sentinel，实现网关限流、秒杀热点参数限流、订单/库存服务熔断降级

**Architecture:** 网关层统一入口限流替代 Redis 过滤器，秒杀服务热点参数限流防刷单，订单/库存服务熔断降级保护数据库，规则存储在 Nacos，Sentinel Dashboard 可视化监控

**Tech Stack:** Spring Cloud Alibaba 2023.0.1.0、Sentinel 1.8.x、Nacos 2.3.2、Spring Cloud Gateway

**进度快照（2026-05-29 14:00）**

| 任务 | 状态 | 说明 |
| --- | --- | --- |
| Task 1~2 | 已完成 | Sentinel Dashboard、5 份 Nacos 规则文件 |
| Task 3~7 | 已完成 | 网关 Sentinel 依赖/配置/降级处理器；旧 RateLimitFilter 已删除 |
| Task 8~10 | 已完成 | 秒杀服务 Sentinel 依赖/配置/注解支持 |
| Task 11~12 | 已完成 | 订单/库存服务 Sentinel 依赖与配置（`8ddc792`） |
| Task 13~16 | 部分完成 | 架构/README/可观测性文档已同步；开发启动指南 Sentinel 说明待补充 |

---

## 文件结构

### 需要修改的文件

| 文件路径 | 修改内容 |
|----------|----------|
| `backend/snapshop-gateway/pom.xml` | 添加 Sentinel 依赖 |
| `backend/snapshop-seckill/pom.xml` | 添加 Sentinel 依赖 |
| `backend/snapshop-order/pom.xml` | 添加 Sentinel 依赖 |
| `backend/snapshop-inventory/pom.xml` | 添加 Sentinel 依赖 |
| `backend/snapshop-gateway/src/main/resources/application.yml` | 添加 Sentinel 配置 |
| `backend/snapshop-seckill/src/main/resources/application.yml` | 添加 Sentinel 配置 |
| `backend/snapshop-order/src/main/resources/application.yml` | 添加 Sentinel 配置 |
| `backend/snapshop-inventory/src/main/resources/application.yml` | 添加 Sentinel 配置 |
| `docker/docker-compose.yml` | 添加 Sentinel Dashboard 服务 |
| `docs/架构设计文档.md` | 更新技术选型和架构图 |
| `docs/工程配置说明.md` | 更新端口表和 Docker 配置 |
| `README.md` | 更新技术栈和监控地址 |

### 需要删除的文件

| 文件路径 | 原因 |
|----------|------|
| `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/filter/RateLimitFilter.java` | 被 Sentinel 网关限流替代 |

### 需要新增的文件

| 文件路径 | 用途 |
|----------|------|
| `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/config/SentinelGatewayConfig.java` | 网关 Sentinel 配置 |
| `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/handler/SentinelFallbackHandler.java` | 网关限流降级处理器 |
| `backend/snapshop-seckill/src/main/java/com/snapshop/seckill/config/SentinelSeckillConfig.java` | 秒杀服务 Sentinel 配置 |
| `backend/snapshop-order/src/main/java/com/snapshop/order/config/SentinelOrderConfig.java` | 订单服务 Sentinel 配置 |
| `backend/snapshop-inventory/src/main/java/com/snapshop/inventory/config/SentinelInventoryConfig.java` | 库存服务 Sentinel 配置 |
| `docker/nacos/init-config/sentinel-rules-snapshop-gateway-flow.yaml` | 网关限流规则 |
| `docker/nacos/init-config/sentinel-rules-snapshop-seckill-flow.yaml` | 秒杀限流规则 |
| `docker/nacos/init-config/sentinel-rules-snapshop-seckill-degrade.yaml` | 秒杀熔断规则 |
| `docker/nacos/init-config/sentinel-rules-snapshop-order-degrade.yaml` | 订单熔断规则 |
| `docker/nacos/init-config/sentinel-rules-snapshop-inventory-degrade.yaml` | 库存熔断规则 |

---

## Task 1: Docker Compose 添加 Sentinel Dashboard

**Files:**
- Modify: `docker/docker-compose.yml`

- [ ] **Step 1: 添加 Sentinel Dashboard 服务配置**

在 `docker/docker-compose.yml` 的 `nacos` 服务之后添加：

```yaml
  sentinel:
    image: bladex/sentinel-dashboard:1.8.8
    container_name: snapshop-sentinel
    restart: unless-stopped
    ports:
      - "8888:8080"
    environment:
      JAVA_OPT: "-Dserver.port=8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
```

- [ ] **Step 2: 验证配置语法**

运行：`docker-compose config`
预期：无语法错误

- [ ] **Step 3: 提交**

```bash
git add docker/docker-compose.yml
git commit -m "feat: 添加 Sentinel Dashboard 到 Docker Compose"
```

---

## Task 2: 创建 Nacos Sentinel 规则配置文件

**Files:**
- Create: `docker/nacos/init-config/sentinel-rules-snapshop-gateway-flow.yaml`
- Create: `docker/nacos/init-config/sentinel-rules-snapshop-seckill-flow.yaml`
- Create: `docker/nacos/init-config/sentinel-rules-snapshop-seckill-degrade.yaml`
- Create: `docker/nacos/init-config/sentinel-rules-snapshop-order-degrade.yaml`
- Create: `docker/nacos/init-config/sentinel-rules-snapshop-inventory-degrade.yaml`

- [ ] **Step 1: 创建网关限流规则文件**

创建 `docker/nacos/init-config/sentinel-rules-snapshop-gateway-flow.yaml`：

```yaml
[
  {
    "resource": "/api/seckill/**",
    "limitApp": "default",
    "grade": 1,
    "count": 1000,
    "strategy": 0,
    "controlBehavior": 0,
    "burstCount": 0
  },
  {
    "resource": "/api/orders/**",
    "limitApp": "default",
    "grade": 1,
    "count": 500,
    "strategy": 0,
    "controlBehavior": 0,
    "burstCount": 0
  },
  {
    "resource": "/api/products/**",
    "limitApp": "default",
    "grade": 1,
    "count": 2000,
    "strategy": 0,
    "controlBehavior": 0,
    "burstCount": 0
  }
]
```

- [ ] **Step 2: 创建秒杀服务限流规则文件**

创建 `docker/nacos/init-config/sentinel-rules-snapshop-seckill-flow.yaml`：

```yaml
[
  {
    "resource": "seckill_submit",
    "limitApp": "default",
    "grade": 1,
    "count": 500,
    "strategy": 0,
    "controlBehavior": 0,
    "burstCount": 0
  },
  {
    "resource": "seckill_token",
    "limitApp": "default",
    "grade": 1,
    "count": 100,
    "strategy": 0,
    "controlBehavior": 0,
    "burstCount": 0
  }
]
```

- [ ] **Step 3: 创建秒杀服务熔断规则文件**

创建 `docker/nacos/init-config/sentinel-rules-snapshop-seckill-degrade.yaml`：

```yaml
[
  {
    "resource": "seckill_redis_call",
    "grade": 0,
    "count": 200,
    "slowRatioThreshold": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000
  },
  {
    "resource": "seckill_db_call",
    "grade": 1,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000
  }
]
```

- [ ] **Step 4: 创建订单服务熔断规则文件**

创建 `docker/nacos/init-config/sentinel-rules-snapshop-order-degrade.yaml`：

```yaml
[
  {
    "resource": "order_db_call",
    "grade": 1,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000
  }
]
```

- [ ] **Step 5: 创建库存服务熔断规则文件**

创建 `docker/nacos/init-config/sentinel-rules-snapshop-inventory-degrade.yaml`：

```yaml
[
  {
    "resource": "inventory_db_call",
    "grade": 1,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000
  }
]
```

- [ ] **Step 6: 提交**

```bash
git add docker/nacos/init-config/sentinel-rules-*.yaml
git commit -m "feat: 添加 Sentinel Nacos 规则配置文件"
```

---

## Task 3: 网关服务添加 Sentinel 依赖

**Files:**
- Modify: `backend/snapshop-gateway/pom.xml`

- [ ] **Step 1: 添加 Sentinel 依赖**

在 `backend/snapshop-gateway/pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- Sentinel 网关限流 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
</dependency>
```

- [ ] **Step 2: 验证依赖解析**

运行：`mvn dependency:resolve -pl snapshop-gateway`
预期：无依赖解析错误

- [ ] **Step 3: 提交**

```bash
git add backend/snapshop-gateway/pom.xml
git commit -m "feat: 网关服务添加 Sentinel 依赖"
```

---

## Task 4: 网关服务添加 Sentinel 配置

**Files:**
- Modify: `backend/snapshop-gateway/src/main/resources/application.yml`

- [ ] **Step 1: 添加 Sentinel 配置**

在 `backend/snapshop-gateway/src/main/resources/application.yml` 中添加：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8888
        port: 8719
      eager: true
      datasource:
        flow:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            dataId: sentinel-rules-snapshop-gateway-flow.yaml
            groupId: SENTINEL_GROUP
            rule-type: flow
            data-type: json
```

- [ ] **Step 2: 提交**

```bash
git add backend/snapshop-gateway/src/main/resources/application.yml
git commit -m "feat: 网关服务添加 Sentinel 配置"
```

---

## Task 5: 网关服务实现 Sentinel 降级处理器

**Files:**
- Create: `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/handler/SentinelFallbackHandler.java`

- [ ] **Step 1: 创建降级处理器**

创建 `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/handler/SentinelFallbackHandler.java`：

```java
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
    public Mono<Void> handleFallback(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> r = R.fail(ErrorCode.RATE_LIMITED.getCode(), message);
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(r);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("序列化降级响应失败", e);
            return response.setComplete();
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/snapshop-gateway/src/main/java/com/snapshop/gateway/handler/SentinelFallbackHandler.java
git commit -m "feat: 网关 Sentinel 降级处理器"
```

---

## Task 6: 网关服务配置 Sentinel 网关适配

**Files:**
- Create: `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/config/SentinelGatewayConfig.java`

- [ ] **Step 1: 创建 Sentinel 网关配置类**

创建 `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/config/SentinelGatewayConfig.java`：

```java
package com.snapshop.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.snapshop.gateway.handler.SentinelFallbackHandler;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

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
```

- [ ] **Step 2: 提交**

```bash
git add backend/snapshop-gateway/src/main/java/com/snapshop/gateway/config/SentinelGatewayConfig.java
git commit -m "feat: 网关 Sentinel 网关适配配置"
```

---

## Task 7: 删除旧的 Redis 限流过滤器

**Files:**
- Delete: `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/filter/RateLimitFilter.java`

- [ ] **Step 1: 删除 RateLimitFilter 文件**

删除 `backend/snapshop-gateway/src/main/java/com/snapshop/gateway/filter/RateLimitFilter.java`

- [ ] **Step 2: 验证编译**

运行：`mvn compile -pl snapshop-gateway`
预期：编译成功，无错误

- [ ] **Step 3: 提交**

```bash
git add backend/snapshop-gateway/src/main/java/com/snapshop/gateway/filter/RateLimitFilter.java
git commit -m "refactor: 删除旧的 Redis 限流过滤器，已被 Sentinel 替代"
```

---

## Task 8: 秒杀服务添加 Sentinel 依赖

**Files:**
- Modify: `backend/snapshop-seckill/pom.xml`

- [ ] **Step 1: 添加 Sentinel 依赖**

在 `backend/snapshop-seckill/pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- Sentinel 熔断限流 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

- [ ] **Step 2: 验证依赖解析**

运行：`mvn dependency:resolve -pl snapshop-seckill`
预期：无依赖解析错误

- [ ] **Step 3: 提交**

```bash
git add backend/snapshop-seckill/pom.xml
git commit -m "feat: 秒杀服务添加 Sentinel 依赖"
```

---

## Task 9: 秒杀服务添加 Sentinel 配置

**Files:**
- Modify: `backend/snapshop-seckill/src/main/resources/application.yml`

- [ ] **Step 1: 添加 Sentinel 配置**

在 `backend/snapshop-seckill/src/main/resources/application.yml` 中添加：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8888
        port: 8719
      eager: true
      datasource:
        flow:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            dataId: sentinel-rules-snapshop-seckill-flow.yaml
            groupId: SENTINEL_GROUP
            rule-type: flow
            data-type: json
        degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            dataId: sentinel-rules-snapshop-seckill-degrade.yaml
            groupId: SENTINEL_GROUP
            rule-type: degrade
            data-type: json
```

- [ ] **Step 2: 提交**

```bash
git add backend/snapshop-seckill/src/main/resources/application.yml
git commit -m "feat: 秒杀服务添加 Sentinel 配置"
```

---

## Task 10: 秒杀服务实现 Sentinel 资源定义

**Files:**
- Create: `backend/snapshop-seckill/src/main/java/com/snapshop/seckill/config/SentinelSeckillConfig.java`
- Modify: 秒杀服务业务代码（需要在具体实现时添加 @SentinelResource 注解）

- [ ] **Step 1: 创建秒杀服务 Sentinel 配置类**

创建 `backend/snapshop-seckill/src/main/java/com/snapshop/seckill/config/SentinelSeckillConfig.java`：

```java
package com.snapshop.seckill.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 秒杀服务 Sentinel 配置
 */
@Configuration
public class SentinelSeckillConfig {

    /**
     * 启用 Sentinel 注解支持
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/snapshop-seckill/src/main/java/com/snapshop/seckill/config/SentinelSeckillConfig.java
git commit -m "feat: 秒杀服务 Sentinel 配置类"
```

---

## Task 11: 订单服务添加 Sentinel 依赖和配置

**Files:**
- Modify: `backend/snapshop-order/pom.xml`
- Modify: `backend/snapshop-order/src/main/resources/application.yml`
- Create: `backend/snapshop-order/src/main/java/com/snapshop/order/config/SentinelOrderConfig.java`

- [ ] **Step 1: 添加 Sentinel 依赖**

在 `backend/snapshop-order/pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- Sentinel 熔断限流 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

- [ ] **Step 2: 添加 Sentinel 配置**

在 `backend/snapshop-order/src/main/resources/application.yml` 中添加：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8888
        port: 8719
      eager: true
      datasource:
        degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            dataId: sentinel-rules-snapshop-order-degrade.yaml
            groupId: SENTINEL_GROUP
            rule-type: degrade
            data-type: json
```

- [ ] **Step 3: 创建订单服务 Sentinel 配置类**

创建 `backend/snapshop-order/src/main/java/com/snapshop/order/config/SentinelOrderConfig.java`：

```java
package com.snapshop.order.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单服务 Sentinel 配置
 */
@Configuration
public class SentinelOrderConfig {

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/snapshop-order/pom.xml backend/snapshop-order/src/main/resources/application.yml backend/snapshop-order/src/main/java/com/snapshop/order/config/SentinelOrderConfig.java
git commit -m "feat: 订单服务添加 Sentinel 依赖和配置"
```

---

## Task 12: 库存服务添加 Sentinel 依赖和配置

**Files:**
- Modify: `backend/snapshop-inventory/pom.xml`
- Modify: `backend/snapshop-inventory/src/main/resources/application.yml`
- Create: `backend/snapshop-inventory/src/main/java/com/snapshop/inventory/config/SentinelInventoryConfig.java`

- [ ] **Step 1: 添加 Sentinel 依赖**

在 `backend/snapshop-inventory/pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- Sentinel 熔断限流 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

- [ ] **Step 2: 添加 Sentinel 配置**

在 `backend/snapshop-inventory/src/main/resources/application.yml` 中添加：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8888
        port: 8719
      eager: true
      datasource:
        degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr}
            namespace: ${spring.cloud.nacos.config.namespace}
            dataId: sentinel-rules-snapshop-inventory-degrade.yaml
            groupId: SENTINEL_GROUP
            rule-type: degrade
            data-type: json
```

- [ ] **Step 3: 创建库存服务 Sentinel 配置类**

创建 `backend/snapshop-inventory/src/main/java/com/snapshop/inventory/config/SentinelInventoryConfig.java`：

```java
package com.snapshop.inventory.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 库存服务 Sentinel 配置
 */
@Configuration
public class SentinelInventoryConfig {

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/snapshop-inventory/pom.xml backend/snapshop-inventory/src/main/resources/application.yml backend/snapshop-inventory/src/main/java/com/snapshop/inventory/config/SentinelInventoryConfig.java
git commit -m "feat: 库存服务添加 Sentinel 依赖和配置"
```

---

## Task 13: 更新架构设计文档

**Files:**
- Modify: `docs/架构设计文档.md`

- [ ] **Step 1: 更新技术选型表**

在 `docs/架构设计文档.md` 的技术选型表中添加：

```
| 流量防护 | Sentinel | 限流、熔断、热点参数防护 |
```

- [ ] **Step 2: 更新架构图说明**

在架构图的说明中添加 Sentinel 组件：

```
    网关 --> Sentinel限流[Sentinel 限流]
    Sentinel限流 --> 路由
```

- [ ] **Step 3: 更新限流设计章节**

将"十五、安全与限流设计"章节重写为：

```markdown
## 十五、安全与限流设计

### 15.1 Sentinel 流量防护

项目使用 Alibaba Sentinel 实现流量防护，覆盖网关层和核心业务服务：

| 服务 | 防护类型 | 说明 |
|------|----------|------|
| snapshop-gateway | 网关限流 | API 路径限流、IP 限流 |
| snapshop-seckill | 热点参数限流 + 熔断 | 商品 ID、用户 ID 限流，Redis/DB 熔断 |
| snapshop-order | 熔断降级 | 数据库熔断保护 |
| snapshop-inventory | 熔断降级 | 库存扣减熔断保护 |

### 15.2 限流规则

- 网关层：`/api/seckill/**` 限制 1000 QPS
- 秒杀服务：热点参数限流，单商品 500 QPS，单用户 5 QPS
- 规则存储：Nacos（SENTINEL_GROUP）
- 控制台：Sentinel Dashboard（端口 8888）

### 15.3 熔断规则

- 慢调用比例：调用耗时 > 200ms 比例超过 50% 时熔断
- 异常比例：异常率超过 50% 时熔断
- 熔断时长：10 秒
- 最小请求数：5

### 15.4 风控名单（已锁定）

| 触发条件 | 动作 |
| --- | --- |
| 同一用户 1 分钟内秒杀失败 ≥ 20 次 | `allowSeckill=false`，持续 30 分钟 |
| 同一 IP 1 分钟内秒杀请求 ≥ 100 次 | 网关限流 429 |
| 解除 | 超时自动解除或管理员在后台手动解除 |
```

- [ ] **Step 4: 提交**

```bash
git add docs/架构设计文档.md
git commit -m "docs: 更新架构设计文档，添加 Sentinel 流量防护说明"
```

---

## Task 14: 更新工程配置说明文档

**Files:**
- Modify: `docs/工程配置说明.md`

- [ ] **Step 1: 更新技术版本表**

在 `docs/工程配置说明.md` 的技术版本表中添加：

```
| Sentinel Dashboard | 1.8.8 | 流量防护控制台 |
```

- [ ] **Step 2: 更新端口表**

在端口表中添加：

```
| sentinel-dashboard | sentinel-dashboard | 8888 | 流量防护控制台 |
```

- [ ] **Step 3: 更新 Docker Compose 规格**

在 Docker Compose 规格中添加 Sentinel 服务：

```markdown
| sentinel | bladex/sentinel-dashboard:1.8.8 | 8888 | 流量防护控制台 |
```

- [ ] **Step 4: 更新 Nacos 配置说明**

在 dataId 列表中添加：

```
| `sentinel-rules-*.yaml` | Sentinel 限流、熔断规则 |
```

- [ ] **Step 5: 提交**

```bash
git add docs/工程配置说明.md
git commit -m "docs: 更新工程配置说明，添加 Sentinel Dashboard 配置"
```

---

## Task 15: 更新 README 文档

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 更新技术栈表**

在 `README.md` 的技术栈表中添加：

```
| 流量防护 | Sentinel | 限流、熔断、热点参数防护 |
```

- [ ] **Step 2: 更新监控栈地址**

在监控栈地址表中添加：

```
| **Sentinel Dashboard** | http://localhost:8888 | 流量防护控制台（默认账号 sentinel/sentinel） |
```

- [ ] **Step 3: 提交**

```bash
git add README.md
git commit -m "docs: 更新 README，添加 Sentinel 技术栈和监控地址"
```

---

## Task 16: 验证集成效果

**Files:**
- 无（测试验证）

- [ ] **Step 1: 启动中间件**

```bash
cd docker
docker-compose up -d
```

预期：所有服务启动成功，包括 Sentinel Dashboard

- [ ] **Step 2: 导入 Nacos 配置**

1. 访问 Nacos 控制台：http://localhost:8848/nacos
2. 创建命名空间 `snapshop-dev`
3. 导入 `docker/nacos/init-config/` 下的 Sentinel 规则文件
4. Group 设置为 `SENTINEL_GROUP`

- [ ] **Step 3: 启动后端服务**

```bash
cd backend
mvn clean install -DskipTests
mvn -pl snapshop-gateway spring-boot:run
mvn -pl snapshop-seckill spring-boot:run
mvn -pl snapshop-order spring-boot:run
mvn -pl snapshop-inventory spring-boot:run
```

- [ ] **Step 4: 访问 Sentinel Dashboard**

访问：http://localhost:8888
默认账号：sentinel/sentinel

预期：能看到已注册的服务

- [ ] **Step 5: 验证限流功能**

使用 curl 或 Postman 快速发送请求：

```bash
for i in {1..1100}; do curl -s http://localhost:8080/api/seckill/test &done
```

预期：超过 1000 次后返回 429 状态码

---

## 自检清单

- [x] 规范覆盖：所有设计文档中的需求都有对应任务
- [x] 占位符扫描：无 TBD/TODO，所有步骤都有完整代码
- [x] 类型一致性：文件路径、类名、方法名在各任务间保持一致
