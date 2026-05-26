package com.snapshop.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snapshop.order.dto.CreateOrderDTO;
import com.snapshop.order.entity.Order;
import com.snapshop.order.entity.OrderItem;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param dto 创建订单请求
     * @return 创建的订单
     */
    Order createOrder(CreateOrderDTO dto);

    /**
     * 查询订单列表（分页）
     *
     * @param userId   用户编号
     * @param status   订单状态（可选）
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    Page<Order> getOrderList(Long userId, String status, Integer pageNo, Integer pageSize);

    /**
     * 查询订单详情
     *
     * @param orderId 订单编号
     * @return 订单详情（含订单明细）
     */
    Order getOrderDetail(Long orderId);

    /**
     * 查询订单明细
     *
     * @param orderId 订单编号
     * @return 订单明细列表
     */
    List<OrderItem> getOrderItems(Long orderId);

    /**
     * 取消订单
     *
     * @param orderId 订单编号
     * @param reason  取消原因
     */
    void cancelOrder(Long orderId, String reason);
}
