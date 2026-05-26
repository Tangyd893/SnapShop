package com.snapshop.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snapshop.admin.entity.LocalMessage;
import com.snapshop.admin.mapper.LocalMessageMapper;
import com.snapshop.admin.service.MqAdminService;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.PageResult;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MqAdminServiceImpl implements MqAdminService {

    @Resource
    private LocalMessageMapper localMessageMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResult<LocalMessage> getDeadLetters(Integer pageNo, Integer pageSize) {
        int current = pageNo != null ? pageNo : 1;
        int size = pageSize != null ? pageSize : 10;

        LambdaQueryWrapper<LocalMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LocalMessage::getStatus, "SEND_FAILED")
                .orderByDesc(LocalMessage::getUpdatedAt);

        Page<LocalMessage> page = new Page<>(current, size);
        Page<LocalMessage> messagePage = localMessageMapper.selectPage(page, wrapper);

        return PageResult.of(messagePage.getRecords(), messagePage.getCurrent(),
                messagePage.getSize(), messagePage.getTotal());
    }

    @Override
    public void requeueDeadLetter(Long messageId) {
        LocalMessage message = localMessageMapper.selectById(messageId);
        if (message == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "消息不存在");
        }

        if (!"SEND_FAILED".equals(message.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅允许重投发送失败的消息");
        }

        rabbitTemplate.convertAndSend(message.getExchangeName(), message.getRoutingKey(), message.getPayload());

        message.setStatus("SENT");
        message.setRetryCount(message.getRetryCount() != null ? message.getRetryCount() + 1 : 1);
        message.setNextRetryTime(null);
        message.setUpdatedAt(LocalDateTime.now());
        localMessageMapper.updateById(message);
    }
}
