package com.snapshop.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.order.entity.Order;
import com.snapshop.order.entity.OrderItem;
import com.snapshop.order.entity.OrderStatusLog;
import com.snapshop.order.dto.InventoryRecoverDTO;
import com.snapshop.order.feign.InventoryFeignClient;
import com.snapshop.order.mapper.OrderItemMapper;
import com.snapshop.order.mapper.OrderMapper;
import com.snapshop.order.mapper.OrderStatusLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 订单超时关闭定时任务
 * <p>
 * 每隔 30 秒扫描一次待支付且已过期的订单，自动关闭并回补库存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final InventoryFeignClient inventoryFeignClient;

    /** 订单状态：待支付 */
    private static final String STATUS_PENDING_PAY = "PENDING_PAY";
    /** 订单状态：已关闭 */
    private static final String STATUS_CLOSED = "CLOSED";

    /**
     * 每 30 秒执行一次超时订单关闭
     */
    @Scheduled(fixedDelay = 30000)
    public void closeTimeoutOrders() {
        log.info("开始执行订单超时关闭任务");

        try {
            // 查询状态为待支付且已过期的订单
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                    .eq(Order::getStatus, STATUS_PENDING_PAY)
                    .lt(Order::getExpireAt, LocalDateTime.now());
            List<Order> timeoutOrders = orderMapper.selectList(wrapper);

            if (timeoutOrders.isEmpty()) {
                log.info("没有需要关闭的超时订单");
                return;
            }

            log.info("发现 {} 个超时订单需要关闭", timeoutOrders.size());

            for (Order order : timeoutOrders) {
                try {
                    closeOrder(order);
                } catch (Exception e) {
                    log.error("订单超时关闭失败: orderId={}, orderNo={}", order.getId(), order.getOrderNo(), e);
                }
            }

            log.info("订单超时关闭任务执行完成");
        } catch (Exception e) {
            log.error("订单超时关闭任务执行异常", e);
        }
    }

    /**
     * 关闭单个超时订单
     */
    private void closeOrder(Order order) {
        log.info("关闭超时订单: orderId={}, orderNo={}", order.getId(), order.getOrderNo());

        // 1. 更新订单状态为已关闭
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(STATUS_CLOSED);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);

        // 2. 记录订单状态流水
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setOrderNo(order.getOrderNo());
        statusLog.setFromStatus(STATUS_PENDING_PAY);
        statusLog.setToStatus(STATUS_CLOSED);
        statusLog.setReason("订单支付超时，系统自动关闭");
        statusLog.setCreatedAt(now);
        orderStatusLogMapper.insert(statusLog);

        // 3. 回补库存
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()));

        for (OrderItem item : orderItems) {
            try {
                // 构造 InventoryRecoverDTO 调用库存回补接口
                InventoryRecoverDTO recoverDTO = new InventoryRecoverDTO();
                recoverDTO.setRequestId(UUID.randomUUID().toString());
                recoverDTO.setBusinessKey(order.getOrderNo() + "_timeout_" + item.getSkuId());
                recoverDTO.setSkuId(item.getSkuId());
                recoverDTO.setQuantity(item.getQuantity());
                recoverDTO.setReason("订单支付超时关闭，回补库存");

                inventoryFeignClient.recover(recoverDTO);
                log.info("库存回补成功: orderId={}, skuId={}, quantity={}",
                        order.getId(), item.getSkuId(), item.getQuantity());
            } catch (Exception e) {
                log.error("库存回补失败: orderId={}, skuId={}, quantity={}",
                        order.getId(), item.getSkuId(), item.getQuantity(), e);
            }
        }

        log.info("超时订单关闭完成: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
    }
}
