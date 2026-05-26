-- =============================================
-- SnapShop 建表脚本
-- =============================================

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户编号',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码摘要',
    `phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    `status` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '用户状态：NORMAL-正常, DISABLED-禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 用户地址表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址编号',
    `user_id` BIGINT NOT NULL COMMENT '用户编号',
    `receiver_name` VARCHAR(64) NOT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(32) NOT NULL COMMENT '收货手机号',
    `province` VARCHAR(64) DEFAULT NULL COMMENT '省份',
    `city` VARCHAR(64) DEFAULT NULL COMMENT '城市',
    `district` VARCHAR(64) DEFAULT NULL COMMENT '区县',
    `detail_address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
    `default_address` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户地址表';

-- ----------------------------
-- 商品分类表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `product_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类编号',
    `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类编号',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- ----------------------------
-- 商品表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品编号',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类编号',
    `title` VARCHAR(128) NOT NULL COMMENT '商品标题',
    `description` TEXT COMMENT '商品描述',
    `cover_url` VARCHAR(512) DEFAULT NULL COMMENT '封面图',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ON_SALE' COMMENT '商品状态：ON_SALE-上架, OFF_SALE-下架',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ----------------------------
-- 商品规格表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sku` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品规格编号',
    `product_id` BIGINT NOT NULL COMMENT '商品编号',
    `sku_name` VARCHAR(128) NOT NULL COMMENT '规格名称',
    `price` BIGINT NOT NULL COMMENT '售价，单位分',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ON_SALE' COMMENT '状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格表';

-- ----------------------------
-- 商品库存表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sku_stock` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存记录编号',
    `sku_id` BIGINT NOT NULL COMMENT '商品规格编号',
    `available_stock` INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    `locked_stock` INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
    `sold_stock` INT NOT NULL DEFAULT 0 COMMENT '已售库存',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品库存表';

-- ----------------------------
-- 秒杀活动表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `seckill_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动编号',
    `activity_name` VARCHAR(128) NOT NULL COMMENT '活动名称',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '活动状态：PENDING-未开始, RUNNING-进行中, ENDED-已结束',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_time` (`start_time`, `end_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';

-- ----------------------------
-- 秒杀活动商品表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `seckill_activity_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动商品编号',
    `activity_id` BIGINT NOT NULL COMMENT '活动编号',
    `sku_id` BIGINT NOT NULL COMMENT '商品规格编号',
    `seckill_price` BIGINT NOT NULL COMMENT '秒杀价，单位分',
    `activity_stock` INT NOT NULL COMMENT '活动库存',
    `limit_per_user` INT NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `status` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_sku` (`activity_id`, `sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动商品表';

-- ----------------------------
-- 秒杀订单关系表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `seckill_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    `order_id` BIGINT NOT NULL COMMENT '订单编号',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户编号',
    `activity_id` BIGINT NOT NULL COMMENT '活动编号',
    `sku_id` BIGINT NOT NULL COMMENT '商品规格编号',
    `request_id` VARCHAR(128) NOT NULL COMMENT '请求编号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_activity_sku` (`user_id`, `activity_id`, `sku_id`),
    UNIQUE KEY `uk_request_id` (`request_id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀订单关系表';

-- ----------------------------
-- 订单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单编号',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户编号',
    `total_amount` BIGINT NOT NULL COMMENT '订单金额，单位分',
    `order_type` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '订单类型：NORMAL-普通订单, SECKILL-秒杀订单',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态：PENDING_PAY-待支付, PAID-已支付, CANCELLED-已取消, CLOSED-已关闭',
    `expire_at` DATETIME DEFAULT NULL COMMENT '支付过期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ----------------------------
-- 订单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细编号',
    `order_id` BIGINT NOT NULL COMMENT '订单编号',
    `sku_id` BIGINT NOT NULL COMMENT '商品规格编号',
    `title` VARCHAR(128) NOT NULL COMMENT '下单时商品标题',
    `quantity` INT NOT NULL COMMENT '数量',
    `price` BIGINT NOT NULL COMMENT '单价，单位分',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- ----------------------------
-- 订单状态流水表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `order_status_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水编号',
    `order_id` BIGINT NOT NULL COMMENT '订单编号',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `from_status` VARCHAR(32) DEFAULT NULL COMMENT '原状态',
    `to_status` VARCHAR(32) NOT NULL COMMENT '新状态',
    `reason` VARCHAR(255) DEFAULT NULL COMMENT '变更原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态流水表';

-- ----------------------------
-- 支付单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付单编号',
    `payment_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
    `order_id` BIGINT NOT NULL COMMENT '订单编号',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户编号',
    `pay_amount` BIGINT NOT NULL COMMENT '支付金额，单位分',
    `pay_type` VARCHAR(32) NOT NULL DEFAULT 'MOCK' COMMENT '支付方式',
    `pay_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '支付状态',
    `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付单表';

-- ----------------------------
-- 库存流水表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `inventory_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水编号',
    `business_key` VARCHAR(128) NOT NULL COMMENT '业务幂等键',
    `sku_id` BIGINT NOT NULL COMMENT '商品规格编号',
    `change_type` VARCHAR(32) NOT NULL COMMENT '变更类型',
    `quantity` INT NOT NULL COMMENT '变更数量',
    `before_available_stock` INT DEFAULT NULL COMMENT '变更前可用库存',
    `after_available_stock` INT DEFAULT NULL COMMENT '变更后可用库存',
    `reason` VARCHAR(255) DEFAULT NULL COMMENT '变更原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_key` (`business_key`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水表';

-- ----------------------------
-- 生产端本地消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `local_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `message_id` VARCHAR(128) NOT NULL COMMENT '消息编号',
    `exchange_name` VARCHAR(128) NOT NULL COMMENT '交换机',
    `routing_key` VARCHAR(128) NOT NULL COMMENT '路由键',
    `payload` TEXT NOT NULL COMMENT '消息内容',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_SEND' COMMENT '消息状态：PENDING_SEND-待发送, SENT-已发送, SEND_FAILED-发送失败, CANCELLED-已取消',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
    `error_message` VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_status_retry_time` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产端本地消息表';

-- ----------------------------
-- 消费端消息日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mq_message_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `message_id` VARCHAR(128) NOT NULL COMMENT '消息编号',
    `business_key` VARCHAR(128) DEFAULT NULL COMMENT '业务幂等键',
    `consumer_group` VARCHAR(128) NOT NULL COMMENT '消费者组',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '消费状态：PROCESSING-处理中, SUCCESS-成功, FAILED-失败, DEAD-死信',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `error_message` VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_consumer` (`message_id`, `consumer_group`),
    KEY `idx_business_key` (`business_key`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消费端消息日志表';
