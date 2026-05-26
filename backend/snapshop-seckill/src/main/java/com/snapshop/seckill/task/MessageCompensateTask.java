package com.snapshop.seckill.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.seckill.entity.LocalMessage;
import com.snapshop.seckill.mapper.LocalMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息补偿重投定时任务
 * <p>
 * 每隔 30 秒扫描本地消息表中待发送的消息，进行重试投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCompensateTask {

    private final LocalMessageMapper localMessageMapper;
    private final RabbitTemplate rabbitTemplate;

    /** 消息状态：待发送 */
    private static final String STATUS_PENDING_SEND = "PENDING_SEND";
    /** 消息状态：已发送 */
    private static final String STATUS_SENT = "SENT";
    /** 消息状态：发送失败 */
    private static final String STATUS_SEND_FAILED = "SEND_FAILED";
    /** 消息状态：已取消 */
    private static final String STATUS_CANCELLED = "CANCELLED";
    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 10;
    /** 失败后重试间隔（秒） */
    private static final long RETRY_DELAY_SECONDS = 60;

    /**
     * 每 30 秒执行一次消息补偿重投
     */
    @Scheduled(fixedDelay = 30000)
    public void compensateMessages() {
        log.info("开始执行消息补偿重投任务");

        try {
            // 查询待发送且到达重试时间的消息
            LambdaQueryWrapper<LocalMessage> wrapper = new LambdaQueryWrapper<LocalMessage>()
                    .eq(LocalMessage::getStatus, STATUS_PENDING_SEND)
                    .lt(LocalMessage::getNextRetryTime, LocalDateTime.now())
                    .lt(LocalMessage::getRetryCount, MAX_RETRY_COUNT);
            List<LocalMessage> messages = localMessageMapper.selectList(wrapper);

            if (messages.isEmpty()) {
                log.info("没有需要补偿重投的消息");
                return;
            }

            log.info("发现 {} 条需要补偿重投的消息", messages.size());

            for (LocalMessage message : messages) {
                try {
                    retrySendMessage(message);
                } catch (Exception e) {
                    log.error("消息补偿重投异常: messageId={}", message.getMessageId(), e);
                }
            }

            log.info("消息补偿重投任务执行完成");
        } catch (Exception e) {
            log.error("消息补偿重投任务执行异常", e);
        }
    }

    /**
     * 重试发送单条消息
     */
    private void retrySendMessage(LocalMessage message) {
        log.info("补偿重投消息: messageId={}, retryCount={}", message.getMessageId(), message.getRetryCount());

        int newRetryCount = (message.getRetryCount() != null ? message.getRetryCount() : 0) + 1;
        LocalDateTime now = LocalDateTime.now();

        try {
            // 重试发送到 RabbitMQ（直接发送原始字节，避免 JSON 字符串双编码）
            Message amqpMessage = new Message(
                    message.getPayload().getBytes(StandardCharsets.UTF_8),
                    new MessageProperties());
            rabbitTemplate.send(
                    message.getExchangeName(),
                    message.getRoutingKey(),
                    amqpMessage);

            // 发送成功，更新状态为已发送
            message.setStatus(STATUS_SENT);
            message.setRetryCount(newRetryCount);
            message.setUpdatedAt(now);
            localMessageMapper.updateById(message);

            log.info("消息补偿重投成功: messageId={}, retryCount={}", message.getMessageId(), newRetryCount);

        } catch (AmqpException e) {
            log.error("消息补偿重投失败: messageId={}, retryCount={}, error={}",
                    message.getMessageId(), newRetryCount, e.getMessage());

            // 判断是否超过最大重试次数
            if (newRetryCount >= MAX_RETRY_COUNT) {
                message.setStatus(STATUS_CANCELLED);
                message.setErrorMessage("超过最大重试次数: " + e.getMessage());
                log.warn("消息超过最大重试次数，标记为已取消: messageId={}", message.getMessageId());
            } else {
                // 未超过，标记发送失败，设置下次重试时间
                message.setStatus(STATUS_SEND_FAILED);
                message.setNextRetryTime(now.plusSeconds(RETRY_DELAY_SECONDS));
                message.setErrorMessage(e.getMessage());
            }

            message.setRetryCount(newRetryCount);
            message.setUpdatedAt(now);
            localMessageMapper.updateById(message);
        }
    }
}
