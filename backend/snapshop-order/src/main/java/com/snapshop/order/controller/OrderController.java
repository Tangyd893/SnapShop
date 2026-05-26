package com.snapshop.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snapshop.common.base.PageResult;
import com.snapshop.common.base.R;
import com.snapshop.order.entity.Order;
import com.snapshop.order.entity.OrderItem;
import com.snapshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务接口
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 查询订单列表（需从请求头获取 userId）
     */
    @GetMapping
    public R<PageResult<Order>> getOrderList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Order> page = orderService.getOrderList(userId, status, pageNo, pageSize);
        PageResult<Order> pageResult = PageResult.of(page.getRecords(), page.getCurrent(),
                page.getSize(), page.getTotal());
        return R.ok(pageResult);
    }

    /**
     * 查询订单详情（含订单明细）
     */
    @GetMapping("/{orderId}")
    public R<Map<String, Object>> getOrderDetail(@PathVariable Long orderId) {
        Order order = orderService.getOrderDetail(orderId);
        List<OrderItem> items = orderService.getOrderItems(orderId);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("userId", order.getUserId());
        data.put("totalAmount", order.getTotalAmount());
        data.put("status", order.getStatus());
        data.put("createdAt", order.getCreatedAt());
        data.put("expireAt", order.getExpireAt());
        data.put("items", items);
        return R.ok(data);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    public R<Boolean> cancelOrder(@PathVariable Long orderId,
                                  @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "用户主动取消");
        orderService.cancelOrder(orderId, reason);
        return R.ok(true);
    }
}
