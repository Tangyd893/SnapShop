package com.snapshop.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表实体，用于可靠消息投递
 */
@Data
@TableName("local_message")
public class LocalMessage {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息编号 */
    private String messageId;

    /** 交换机 */
    private String exchangeName;

    /** 路由键 */
    private String routingKey;

    /** 消息内容（JSON） */
    private String payload;

    /** 消息状态：待发送 / 已发送 / 发送失败 */
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
