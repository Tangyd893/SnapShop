package com.snapshop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 消息消费日志实体，对应 mq_message_log 表
 */
@Data
@TableName("mq_message_log")
public class MqMessageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息编号 */
    private String messageId;

    /** 业务键 */
    private String businessKey;

    /** 消费者组 */
    private String consumerGroup;

    /** 消费状态：PROCESSING-处理中，SUCCESS-成功，FAILED-失败 */
    private String status;

    /** 重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
