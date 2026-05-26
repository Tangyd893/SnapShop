package com.snapshop.admin.service;

import com.snapshop.admin.entity.Order;
import com.snapshop.common.base.PageResult;

import java.util.Map;

/**
 * 管理后台订单服务接口
 */
public interface OrderAdminService {

    /**
     * 订单分页查询
     */
    PageResult<Order> getOrderList(String status, Long userId, String startTime, String endTime, Integer pageNo, Integer pageSize);

    /**
     * 订单详情（含状态流水和明细）
     */
    Map<String, Object> getOrderDetail(Long orderId);
}
