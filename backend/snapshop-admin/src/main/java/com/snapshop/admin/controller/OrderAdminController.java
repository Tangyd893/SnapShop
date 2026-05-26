package com.snapshop.admin.controller;

import com.snapshop.admin.entity.Order;
import com.snapshop.admin.service.OrderAdminService;
import com.snapshop.common.base.PageResult;
import com.snapshop.common.base.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台订单控制器（T-1321）
 */
@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    @Resource
    private OrderAdminService orderAdminService;

    /**
     * 订单分页查询
     */
    @GetMapping
    public R<PageResult<Order>> getOrderList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PageResult<Order> result = orderAdminService.getOrderList(status, userId, startTime, endTime, pageNo, pageSize);
        return R.ok(result);
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public R<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        return R.ok(orderAdminService.getOrderDetail(id));
    }
}
