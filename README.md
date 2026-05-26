# SnapShop 在线购物秒杀平台

SnapShop 是一个在线购物秒杀平台，后端规划使用 Java、Spring Cloud Alibaba、Nacos、OpenFeign、RabbitMQ，前端规划使用 Vue3，中间件通过 Docker 管理。

项目重点关注秒杀场景下的高并发处理、异步下单、库存一致性、消息可靠投递、重复消费防护和失败补偿。

## 项目仓库

[Tangyd893/SnapShop](https://github.com/Tangyd893/SnapShop.git)

## 目录结构

```text
SnapShop/
├── backend/                 # 后端微服务工程
├── frontend/                # Vue3 前端工程
├── docker/                  # 中间件 Docker 配置
├── docs/                    # 项目文档
├── testing/                 # 接口测试、压测脚本、测试数据
└── README.md                # 项目说明
```

## 后端模块规划

```text
backend/
├── snapshop-gateway/        # 网关服务
├── snapshop-common/         # 公共模块
├── snapshop-auth/           # 认证服务
├── snapshop-user/           # 用户服务
├── snapshop-product/        # 商品服务
├── snapshop-seckill/        # 秒杀服务
├── snapshop-order/          # 订单服务
├── snapshop-inventory/      # 库存服务
└── snapshop-payment/        # 支付服务或模拟支付服务
```

## 前端模块规划

```text
frontend/
└── snapshop-web/            # Vue3 + Vite 前端应用
```

## 文档入口

### 核心设计

- [架构设计文档](docs/架构设计文档.md)
- [接口设计文档](docs/接口设计文档.md)
- [数据库设计文档](docs/数据库设计文档.md)
- [RabbitMQ 可靠消息设计文档](docs/RabbitMQ可靠消息设计文档.md)

### 开发与交付

- [完整增强版范围说明](docs/完整增强版范围说明.md)
- [工程配置说明](docs/工程配置说明.md)
- [开发启动指南](docs/开发启动指南.md)
- [项目里程碑文档](docs/项目里程碑文档.md)
- [开发任务拆解文档](docs/开发任务拆解文档.md)
- [文档一致性检查清单](docs/文档一致性检查清单.md)

### 完整增强版专项

- [管理后台设计文档](docs/管理后台设计文档.md)
- [可观测性与部署文档](docs/可观测性与部署文档.md)

## 推荐开工顺序

1. 阅读 [完整增强版范围说明](docs/完整增强版范围说明.md)，确认交付边界。
2. 阅读 [工程配置说明](docs/工程配置说明.md)，锁定版本、端口与 Nacos 约定。
3. 执行 [文档一致性检查清单](docs/文档一致性检查清单.md) 自检。
4. 阅读 [开发启动指南](docs/开发启动指南.md)，按 T-001 起动工。
5. 阅读 [项目里程碑文档](docs/项目里程碑文档.md) 与 [开发任务拆解文档](docs/开发任务拆解文档.md)。
6. 实现阶段对照数据库、RabbitMQ、接口、管理后台、可观测性各文档。

