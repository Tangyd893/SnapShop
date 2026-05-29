# SnapShop 在线购物秒杀平台

SnapShop 是一个面向高并发秒杀场景的在线购物平台，后端采用 **Java + Spring Cloud Alibaba** 微服务架构，前端采用 **Vue3 + Vite**，通过 **Docker** 管理本地中间件环境。项目重点关注秒杀场景下的高并发处理、异步下单、库存一致性、消息可靠投递、重复消费防护和失败补偿。

**项目状态（2026-05-29）**：第一至七阶段代码已完成；Sentinel 集成与 CI 门禁已补齐；冒烟脚本与安全基线清单已产出。详情见 [项目进度追踪](docs/项目进度追踪.md)。

## 技术栈

| 类型 | 技术 | 用途 |
| --- | --- | --- |
| 后端框架 | Spring Boot 3.x | 微服务基础框架 |
| 微服务治理 | Spring Cloud Alibaba | 服务注册发现、配置管理、网关 |
| 注册配置中心 | Nacos | 服务注册与配置管理 |
| 流量防护 | Sentinel | 网关限流、秒杀流控/熔断、服务降级 |
| 服务间调用 | OpenFeign | 声明式远程调用 |
| 消息队列 | RabbitMQ | 秒杀削峰、异步下单、事件通知 |
| 缓存 | Redis | 秒杀库存预扣、幂等标记、热点数据缓存 |
| 数据库 | MySQL | 核心业务数据持久化 |
| ORM | MyBatis-Plus | 数据访问层 |
| 前端框架 | Vue3 | 页面渲染与用户交互 |
| 前端构建 | Vite | 开发服务器与构建打包 |
| 前端路由 | Vue Router | 页面路由管理 |
| 状态管理 | Pinia | 全局状态管理 |
| HTTP 库 | Axios | 前端请求封装 |
| 容器管理 | Docker & Docker Compose | 中间件环境管理 |
| 测试框架 | JUnit 5、Spring Boot Test | 单元测试与集成测试 |
| 压力测试 | JMeter / Gatling | 秒杀场景压测 |

## 项目结构

```text
SnapShop/
├── backend/                          # 后端微服务工程
│   ├── pom.xml                       # Maven 父 POM（依赖与插件版本管理）
│   ├── snapshop-common/              # 公共模块（统一响应、异常、工具类）
│   ├── snapshop-gateway/             # 网关服务（路由转发、鉴权、限流）
│   ├── snapshop-auth/                # 认证服务（注册、登录、令牌管理）
│   ├── snapshop-user/                # 用户服务（用户资料、收货地址）
│   ├── snapshop-product/             # 商品服务（商品列表、详情、分类）
│   ├── snapshop-seckill/             # 秒杀服务（活动管理、秒杀令牌、秒杀提交）
│   ├── snapshop-order/               # 订单服务（订单创建、查询、取消）
│   ├── snapshop-inventory/           # 库存服务（库存扣减、回补、对账）
│   ├── snapshop-payment/             # 支付服务（模拟支付）
│   └── snapshop-admin/               # 管理后台 API（商品/秒杀/订单运维）
├── frontend/                         # Vue3 前端工程
│   ├── snapshop-web/                 # C 端用户应用
│   └── snapshop-admin/               # 管理后台前端（13 个页面）
├── docker/                           # 中间件 Docker 配置
│   ├── docker-compose.yml            # 本地中间件编排
│   ├── nacos/                        # Nacos 配置
│   ├── rabbitmq/                     # RabbitMQ 配置
│   ├── mysql/                        # MySQL 初始化脚本
│   └── redis/                        # Redis 配置
├── docs/                             # 项目设计文档
│   ├── 架构设计文档.md
│   ├── 接口设计文档.md
│   ├── 数据库设计文档.md
│   ├── RabbitMQ可靠消息设计文档.md
│   ├── 工程配置说明.md
│   ├── 开发启动指南.md
│   ├── 开发任务拆解文档.md
│   └── ...
├── testing/                          # 测试相关
│   ├── api/                          # 接口测试脚本
│   ├── load/                         # 压测脚本
│   └── data/                         # 测试数据
└── README.md                         # 项目说明（本文件）
```

## 快速启动

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- Docker & Docker Compose

### 2. 启动中间件（Docker）

```bash
cd docker
docker-compose up -d
```

### 3. 初始化数据库

```bash
# 连接 MySQL 并执行初始化 SQL
mysql -h 127.0.0.1 -u root -p < docker/mysql/init/01-schema.sql
mysql -h 127.0.0.1 -u root -p < docker/mysql/init/02-data.sql
```

### 4. 导入 Nacos 配置

```bash
# 通过 Nacos 控制台或 API 导入各服务的配置
# 访问 http://localhost:8848/nacos
```

### 5. 启动后端服务

```bash
cd backend

# 编译所有模块
mvn clean install -DskipTests

# 按顺序启动（建议使用多个终端）
mvn -pl snapshop-gateway spring-boot:run
mvn -pl snapshop-auth spring-boot:run
mvn -pl snapshop-user spring-boot:run
mvn -pl snapshop-product spring-boot:run
mvn -pl snapshop-seckill spring-boot:run
mvn -pl snapshop-order spring-boot:run
mvn -pl snapshop-inventory spring-boot:run
mvn -pl snapshop-payment spring-boot:run
```

### 6. 启动前端

```bash
cd frontend/snapshop-web
npm install
npm run dev
```

### 7. 运行冒烟测试

```bash
# 需先启动中间件与全部后端服务
cd testing/api
chmod +x smoke-test.sh
./smoke-test.sh
# 报告输出至 testing/reports/smoke-*.txt
```

## API 接口概览

所有接口通过网关访问，基础路径前缀为 `/api`，统一响应格式：

```json
{
  "code": 0,
  "message": "成功",
  "data": {},
  "requestId": "REQ...",
  "timestamp": "2026-05-27 10:00:00"
}
```

| 模块 | 路径前缀 | 主要接口 |
| --- | --- | --- |
| 认证服务 | `/api/auth` | 注册、登录、刷新令牌、退出 |
| 用户服务 | `/api/users` | 查询/修改用户资料、收货地址管理 |
| 商品服务 | `/api/products` | 商品列表、商品详情、商品分类 |
| 秒杀服务 | `/api/seckill` | 活动列表/详情、获取秒杀令牌、提交秒杀、查询秒杀结果 |
| 订单服务 | `/api/orders` | 订单列表、订单详情、取消订单 |
| 支付服务 | `/api/payments` | 创建支付单、模拟支付、查询支付状态 |

## 核心业务流程

### 秒杀提交流程

```
用户点击秒杀 → 校验活动状态 → 获取秒杀令牌 → 提交秒杀请求
  → Redis 预扣库存 → RabbitMQ 投递下单消息 → 返回"排队中"
  → 订单服务消费消息 → MySQL 扣减库存 → 创建订单
  → 写入秒杀结果到 Redis → 前端轮询获取结果
```

### 库存一致性保障

- **Redis 预扣**：秒杀入口通过 Lua 脚本原子扣减 Redis 库存
- **MySQL 条件更新**：订单创建时通过乐观锁条件扣减（`WHERE available_stock >= ? AND version = ?`）
- **库存回补**：订单取消/超时/失败时同时回补 MySQL 和 Redis 库存
- **定时对账**：每日凌晨 2 点对比 Redis 与 MySQL 库存差异，超阈值告警

### 消息可靠性

- **生产端**：本地消息表 + 发布确认，保证消息不丢失
- **消费端**：手动确认 + 消息幂等 + 业务幂等 + 数据库唯一约束，防止重复消费
- **死信处理**：多次重试失败的消息进入死信队列，人工介入处理

## 开发文档索引

### 核心设计

- [架构设计文档](docs/架构设计文档.md)
- [接口设计文档](docs/接口设计文档.md)
- [数据库设计文档](docs/数据库设计文档.md)
- [RabbitMQ 可靠消息设计文档](docs/RabbitMQ可靠消息设计文档.md)

### 开发与交付

- [完整增强版范围说明](docs/完整增强版范围说明.md)
- [工程配置说明](docs/工程配置说明.md)
- [开发启动指南](docs/开发启动指南.md)
- [项目进度追踪](docs/项目进度追踪.md)
- [安全基线检查清单](docs/安全基线检查清单.md)
- [Sentinel 集成设计](docs/superpowers/specs/2026-05-27-sentinel-integration-design.md)
- [项目里程碑文档](docs/项目里程碑文档.md)
- [开发任务拆解文档](docs/开发任务拆解文档.md)
- [文档一致性检查清单](docs/文档一致性检查清单.md)

### 完整增强版专项

- [管理后台设计文档](docs/管理后台设计文档.md)
- [可观测性与部署文档](docs/可观测性与部署文档.md)

## 推荐开工顺序

1. 阅读 [完整增强版范围说明](docs/完整增强版范围说明.md)，确认交付边界
2. 阅读 [工程配置说明](docs/工程配置说明.md)，锁定版本、端口与 Nacos 约定
3. 执行 [文档一致性检查清单](docs/文档一致性检查清单.md) 自检
4. 阅读 [项目进度追踪](docs/项目进度追踪.md)，确认当前阶段、已完成项和待验证项
5. 阅读 [开发启动指南](docs/开发启动指南.md)，按任务编号起工
6. 阅读 [项目里程碑文档](docs/项目里程碑文档.md) 与 [开发任务拆解文档](docs/开发任务拆解文档.md)
7. 实现阶段对照各专项设计文档进行开发

## 部署说明

### 本地开发完整启动步骤

1. **环境准备**：确保已安装 JDK 21、Maven 3.8+、Node.js 20+、Docker & Docker Compose。

2. **启动中间件**（MySQL、Redis、RabbitMQ、Nacos）：
   ```bash
   cd docker
   docker-compose up -d
   ```

3. **初始化数据库**：
   ```bash
   mysql -h 127.0.0.1 -u root -p < docker/mysql/init/01-schema.sql
   mysql -h 127.0.0.1 -u root -p < docker/mysql/init/02-data.sql
   ```

4. **导入 Nacos 配置**：启动 Nacos 后，通过控制台 `http://localhost:8848/nacos` 导入 `docker/nacos/init-config/` 下的配置到 `snapshop-dev` 命名空间。

5. **启动后端服务**（按依赖顺序）：
   ```bash
   cd backend
   mvn clean install -DskipTests
   # 以下在多个终端窗口分别启动
   mvn -pl snapshop-gateway spring-boot:run
   mvn -pl snapshop-auth spring-boot:run
   mvn -pl snapshop-user spring-boot:run
   mvn -pl snapshop-product spring-boot:run
   mvn -pl snapshop-seckill spring-boot:run
   mvn -pl snapshop-order spring-boot:run
   mvn -pl snapshop-inventory spring-boot:run
   mvn -pl snapshop-payment spring-boot:run
   mvn -pl snapshop-admin spring-boot:run   # 管理后台（可选）
   ```

6. **启动前端**：
   ```bash
   # C 端
   cd frontend/snapshop-web
   npm install
   npm run dev

   # 管理后台（可选）
   cd frontend/snapshop-admin
   npm install
   npm run dev
   ```

### Docker Compose Profiles 说明

| Profile | 包含服务 | 启动命令 |
| --- | --- | --- |
| 默认（无 profile） | MySQL、Redis、RabbitMQ、Nacos、Sentinel Dashboard | `docker-compose up -d` |
| `monitoring` | Prometheus、Grafana | `docker-compose --profile monitoring up -d` |
| `tracing` | Jaeger（链路追踪） | `docker-compose --profile tracing up -d` |

同时启动所有服务：`docker-compose --profile monitoring --profile tracing up -d`

### 监控栈地址

| 组件 | 地址 | 说明 |
| --- | --- | --- |
| **Sentinel Dashboard** | http://localhost:8888 | 流量防护控制台（默认账号 sentinel/sentinel） |
| **Grafana** | http://localhost:3000 | 可视化仪表盘（默认账号 admin/admin） |
| **Prometheus** | http://localhost:9090 | 指标采集与告警 |
| **Jaeger** | http://localhost:16686 | 分布式链路追踪 UI |
| **RabbitMQ 管理** | http://localhost:15672 | 消息队列管理界面 |
| **Nacos 控制台** | http://localhost:8848/nacos | 服务注册与配置管理 |

详细可观测性设计见 [可观测性与部署文档](docs/可观测性与部署文档.md)。
