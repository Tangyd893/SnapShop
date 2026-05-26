package com.snapshop.admin.service;

import com.snapshop.admin.entity.LocalMessage;
import com.snapshop.common.base.PageResult;

/**
 * 管理后台 MQ 死信运维服务接口
 */
public interface MqAdminService {

    /**
     * 死信消息分页查询
     */
    PageResult<LocalMessage> getDeadLetters(Integer pageNo, Integer pageSize);

    /**
     * 死信消息重投
     */
    void requeueDeadLetter(Long messageId);
}
