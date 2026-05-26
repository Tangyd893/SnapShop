package com.snapshop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单明细实体，对应 order_item 表
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private Long orderId;

    /** 商品规格编号 */
    private Long skuId;

    /** 下单时商品标题 */
    private String title;

    /** 数量 */
    private Integer quantity;

    /** 单价，单位分 */
    private Long price;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
