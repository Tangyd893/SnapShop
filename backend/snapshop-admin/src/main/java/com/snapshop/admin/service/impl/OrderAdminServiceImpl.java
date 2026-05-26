package com.snapshop.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snapshop.admin.entity.Order;
import com.snapshop.admin.entity.OrderItem;
import com.snapshop.admin.entity.OrderStatusLog;
import com.snapshop.admin.mapper.OrderItemMapper;
import com.snapshop.admin.mapper.OrderMapper;
import com.snapshop.admin.mapper.OrderStatusLogMapper;
import com.snapshop.admin.service.OrderAdminService;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderAdminServiceImpl implements OrderAdminService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;

    @Override
    public PageResult<Order> getOrderList(String status, Long userId, String startTime, String endTime,
                                           Integer pageNo, Integer pageSize) {
        int current = pageNo != null ? pageNo : 1;
        int size = pageSize != null ? pageSize : 10;

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(Order::getCreatedAt, startTime);
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(Order::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> page = new Page<>(current, size);
        Page<Order> orderPage = orderMapper.selectPage(page, wrapper);

        return PageResult.of(orderPage.getRecords(), orderPage.getCurrent(),
                orderPage.getSize(), orderPage.getTotal());
    }

    @Override
    public Map<String, Object> getOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        LambdaQueryWrapper<OrderStatusLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getCreatedAt);
        List<OrderStatusLog> statusLogs = orderStatusLogMapper.selectList(logWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("userId", order.getUserId());
        data.put("totalAmount", order.getTotalAmount());
        data.put("orderType", order.getOrderType());
        data.put("status", order.getStatus());
        data.put("expireAt", order.getExpireAt());
        data.put("createdAt", order.getCreatedAt());
        data.put("updatedAt", order.getUpdatedAt());
        data.put("items", items);
        data.put("statusLogs", statusLogs);

        return data;
    }
}
