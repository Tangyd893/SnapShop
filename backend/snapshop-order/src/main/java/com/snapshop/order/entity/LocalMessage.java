package com.snapshop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表实体，对应 local_message 表
 */
@Data
@TableName("local_message")
public class LocalMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息编号（全局唯一） */
    private String messageId;

    /** 交换机名称 */
    private String exchangeName;

    /** 路由键 */
    private String routingKey;

    /** 消息负载（JSON） */
    private String payload;

    /** 消息状态：PENDING-待发送，SENT-已发送，FAILED-发送失败 */
    private String status;

    /** 重试次数 */
    private Integer retryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
