package com.snapshop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.order.dto.CreateOrderDTO;
import com.snapshop.order.entity.*;
import com.snapshop.order.mapper.*;
import com.snapshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 订单类型：普通订单 */
    private static final String ORDER_TYPE_NORMAL = "NORMAL";
    /** 订单类型：秒杀订单 */
    private static final String ORDER_TYPE_SECKILL = "SECKILL";
    /** 订单状态：待支付 */
    private static final String STATUS_PENDING_PAY = "PENDING_PAY";
    /** 订单状态：已取消 */
    private static final String STATUS_CANCELLED = "CANCELLED";
    /** Redis 秒杀结果键前缀 */
    private static final String SECKILL_RESULT_PREFIX = "seckill:result:";
    /** 秒杀结果 TTL（秒） */
    private static final long SECKILL_RESULT_TTL = 86400L;
    /** 订单支付过期时间（分钟） */
    private static final int ORDER_EXPIRE_MINUTES = 15;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderDTO dto) {
        log.info("创建订单: userId={}, skuId={}, orderType={}, requestId={}",
                dto.getUserId(), dto.getSkuId(), dto.getOrderType(), dto.getRequestId());

        // 生成订单号
        String orderNo = generateOrderNo();

        // 计算总金额
        long totalAmount = dto.getPrice() * dto.getQuantity();
        LocalDateTime now = LocalDateTime.now();

        // 创建订单记录
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(dto.getUserId());
        order.setTotalAmount(totalAmount);
        order.setOrderType(dto.getOrderType() != null ? dto.getOrderType() : ORDER_TYPE_NORMAL);
        order.setStatus(STATUS_PENDING_PAY);
        order.setExpireAt(now.plusMinutes(ORDER_EXPIRE_MINUTES));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        // 创建订单明细
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setSkuId(dto.getSkuId());
        orderItem.setTitle(dto.getTitle());
        orderItem.setQuantity(dto.getQuantity());
        orderItem.setPrice(dto.getPrice());
        orderItem.setCreatedAt(now);
        orderItemMapper.insert(orderItem);

        // 秒杀订单：创建秒杀订单关系记录
        if (dto.getActivityId() != null || ORDER_TYPE_SECKILL.equals(dto.getOrderType())) {
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setOrderId(order.getId());
            seckillOrder.setOrderNo(orderNo);
            seckillOrder.setUserId(dto.getUserId());
            seckillOrder.setActivityId(dto.getActivityId());
            seckillOrder.setSkuId(dto.getSkuId());
            seckillOrder.setRequestId(dto.getRequestId());
            seckillOrder.setCreatedAt(now);
            seckillOrderMapper.insert(seckillOrder);
        }

        // 记录订单状态流水
        saveOrderStatusLog(order.getId(), orderNo, null, STATUS_PENDING_PAY,
                dto.getOrderType() != null ? "创建" + dto.getOrderType() + "订单" : "创建订单");

        // 秒杀结果写入 Redis（当 requestId 不为空时）
        if (dto.getRequestId() != null && !dto.getRequestId().isEmpty()) {
            writeSeckillResult(dto.getRequestId(), "成功", order.getId(), orderNo, null);
        }

        log.info("订单创建成功: orderId={}, orderNo={}", order.getId(), orderNo);
        return order;
    }

    @Override
    public Page<Order> getOrderList(Long userId, String status, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null && !status.isEmpty(), Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt);

        return orderMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public Order getOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reason) {
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 只有待支付订单可以取消
        if (!STATUS_PENDING_PAY.equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // 更新订单状态
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(STATUS_CANCELLED);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);

        // 记录状态流水
        saveOrderStatusLog(orderId, order.getOrderNo(), STATUS_PENDING_PAY, STATUS_CANCELLED,
                reason != null ? reason : "用户主动取消");

        log.info("订单取消成功: orderId={}, orderNo={}, reason={}", orderId, order.getOrderNo(), reason);
    }

    /**
     * 生成订单号，格式：SO + yyyyMMdd + 6位随机数字
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "SO" + dateStr + randomNum;
    }

    /**
     * 写入秒杀结果到 Redis
     */
    private void writeSeckillResult(String requestId, String resultStatus,
                                    Long orderId, String orderNo, String failureReason) {
        try {
            String key = SECKILL_RESULT_PREFIX + requestId;
            String value = String.format(
                    "{\"requestId\":\"%s\",\"resultStatus\":\"%s\",\"orderId\":%d,\"orderNo\":\"%s\",\"failureReason\":%s}",
                    requestId, resultStatus, orderId, orderNo,
                    failureReason != null ? "\"" + failureReason + "\"" : "null");
            stringRedisTemplate.opsForValue().set(key, value, SECKILL_RESULT_TTL, TimeUnit.SECONDS);
            log.info("秒杀结果写入 Redis 成功: requestId={}, result={}", requestId, resultStatus);
        } catch (Exception e) {
            log.error("秒杀结果写入 Redis 失败: requestId={}", requestId, e);
        }
    }

    /**
     * 保存订单状态流水
     */
    private void saveOrderStatusLog(Long orderId, String orderNo,
                                    String fromStatus, String toStatus, String reason) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(orderId);
        statusLog.setOrderNo(orderNo);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setReason(reason);
        statusLog.setCreatedAt(LocalDateTime.now());
        orderStatusLogMapper.insert(statusLog);
    }
}
