package com.snapshop.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 消息消费日志实体（管理后台直连）
 */
@Data
@TableName("mq_message_log")
public class MqMessageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String businessKey;

    private String consumerGroup;

    private String status;

    private Integer retryCount;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
