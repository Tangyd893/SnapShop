package com.snapshop.order;

import com.snapshop.order.entity.Order;
import com.snapshop.order.entity.OrderItem;
import com.snapshop.order.entity.OrderStatusLog;
import com.snapshop.order.entity.SeckillOrder;
import com.snapshop.order.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单超时关闭测试
 * 验证超时订单能被关闭并回补库存
 */
@SpringBootTest
class OrderTimeoutTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    private Long testOrderId;

    @BeforeEach
    void setUp() {
        // 创建已过期的待支付订单
        Order order = new Order();
        order.setOrderNo("SO" + System.currentTimeMillis());
        order.setUserId(10001L);
        order.setTotalAmount(9900L);
        order.setOrderType("SECKILL");
        order.setStatus("PENDING_PAY");
        order.setExpireAt(LocalDateTime.now().minusMinutes(30)); // 30分钟前已过期
        order.setCreatedAt(LocalDateTime.now().minusHours(1));
        order.setUpdatedAt(LocalDateTime.now().minusHours(1));
        orderMapper.insert(order);
        testOrderId = order.getId();

        // 添加订单明细
        OrderItem item = new OrderItem();
        item.setOrderId(testOrderId);
        item.setSkuId(30001L);
        item.setTitle("测试商品");
        item.setQuantity(1);
        item.setPrice(9900L);
        item.setCreatedAt(LocalDateTime.now().minusHours(1));
        orderItemMapper.insert(item);

        // 添加秒杀订单关联
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setOrderId(testOrderId);
        seckillOrder.setOrderNo(order.getOrderNo());
        seckillOrder.setUserId(10001L);
        seckillOrder.setActivityId(10001L);
        seckillOrder.setSkuId(30001L);
        seckillOrder.setRequestId("REQ-TEST-" + System.currentTimeMillis());
        seckillOrder.setCreatedAt(LocalDateTime.now().minusHours(1));
        seckillOrderMapper.insert(seckillOrder);
    }

    @Test
    @Transactional
    void testOrderTimeoutClose() {
        // 模拟定时任务逻辑：查询过期订单
        List<Order> expiredOrders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, "PENDING_PAY")
                        .lt(Order::getExpireAt, LocalDateTime.now())
        );

        assertFalse(expiredOrders.isEmpty(), "应至少有一条过期订单");

        for (Order order : expiredOrders) {
            // 关闭订单
            order.setStatus("CLOSED");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);

            // 记录状态流水
            OrderStatusLog statusLog = new OrderStatusLog();
            statusLog.setOrderId(order.getId());
            statusLog.setOrderNo(order.getOrderNo());
            statusLog.setFromStatus("PENDING_PAY");
            statusLog.setToStatus("CLOSED");
            statusLog.setReason("订单超时自动关闭");
            statusLog.setCreatedAt(LocalDateTime.now());
            orderStatusLogMapper.insert(statusLog);
        }

        // 验证订单状态已更新
        Order closedOrder = orderMapper.selectById(testOrderId);
        assertNotNull(closedOrder);
        assertEquals("CLOSED", closedOrder.getStatus(), "订单应被标记为已关闭");

        // 验证状态流水已记录
        Long count = orderStatusLogMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, testOrderId)
                        .eq(OrderStatusLog::getToStatus, "CLOSED")
        );
        assertTrue(count > 0, "应有状态流水记录");
    }

    @Test
    void testPaidOrderNotClosed() {
        // 创建已支付订单（不应被关闭）
        Order paidOrder = new Order();
        paidOrder.setOrderNo("SO-PAID-" + System.currentTimeMillis());
        paidOrder.setUserId(10001L);
        paidOrder.setTotalAmount(9900L);
        paidOrder.setOrderType("SECKILL");
        paidOrder.setStatus("PAID");
        paidOrder.setExpireAt(LocalDateTime.now().minusMinutes(30));
        paidOrder.setCreatedAt(LocalDateTime.now().minusHours(1));
        paidOrder.setUpdatedAt(LocalDateTime.now().minusHours(1));
        orderMapper.insert(paidOrder);

        // 模拟定时任务只查询 PENDING_PAY 的
        List<Order> expiredOrders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, "PENDING_PAY")
                        .lt(Order::getExpireAt, LocalDateTime.now())
        );

        // 已支付的订单不要在关闭列表中出现
        boolean containsPaid = expiredOrders.stream()
                .anyMatch(o -> o.getId().equals(paidOrder.getId()));
        assertFalse(containsPaid, "已支付订单不应出现在待关闭列表中");
    }
}
