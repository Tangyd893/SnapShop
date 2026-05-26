-- =============================================
-- SnapShop 初始化数据脚本
-- =============================================

-- 测试用户（密码为 123456 的 BCrypt 哈希）
INSERT INTO `user` (`id`, `username`, `password_hash`, `phone`, `nickname`, `status`) VALUES
(10001, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000001', '张三', 'NORMAL'),
(10002, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000002', '李四', 'NORMAL');

-- 商品分类
INSERT INTO `product_category` (`id`, `category_name`, `parent_id`, `sort`) VALUES
(1, '数码产品', 0, 1),
(2, '家居生活', 0, 2);

-- 商品
INSERT INTO `product` (`id`, `category_id`, `title`, `description`, `cover_url`, `status`) VALUES
(20001, 1, '秒杀测试商品-无线耳机', '高品质降噪无线耳机，支持蓝牙5.3', '/images/headphone.png', 'ON_SALE'),
(20002, 2, '秒杀测试商品-智能台灯', '护眼智能台灯，支持亮度调节', '/images/lamp.png', 'ON_SALE');

-- 商品规格表
INSERT INTO `sku` (`id`, `product_id`, `sku_name`, `price`, `status`) VALUES
(30001, 20001, '标准款', 19900, 'ON_SALE'),
(30002, 20001, '高配款', 29900, 'ON_SALE'),
(30003, 20002, '经典款', 9900, 'ON_SALE');

-- 商品库存
INSERT INTO `sku_stock` (`id`, `sku_id`, `available_stock`, `locked_stock`, `sold_stock`, `version`) VALUES
(1, 30001, 1000, 0, 0, 0),
(2, 30002, 500, 0, 0, 0),
(3, 30003, 800, 0, 0, 0);

-- 秒杀活动（一个进行中，活动时间设为未来很长一段）
INSERT INTO `seckill_activity` (`id`, `activity_name`, `start_time`, `end_time`, `status`) VALUES
(10001, '整点秒杀', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 30 DAY), 'RUNNING');

-- 秒杀活动商品
INSERT INTO `seckill_activity_item` (`id`, `activity_id`, `sku_id`, `seckill_price`, `activity_stock`, `limit_per_user`, `status`) VALUES
(1, 10001, 30001, 9900, 100, 1, 'NORMAL'),
(2, 10001, 30003, 4900, 50, 1, 'NORMAL');
