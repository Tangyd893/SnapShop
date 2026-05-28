package com.snapshop.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.R;
import com.snapshop.order.config.RabbitConfig;
import com.snapshop.order.dto.CreateOrderDTO;
import com.snapshop.order.dto.InventoryDeductDTO;
import com.snapshop.order.dto.InventoryResponseDTO;
import com.snapshop.order.entity.*;
import com.snapshop.order.feign.InventoryFeignClient;
import com.snapshop.order.mapper.*;
import com.snapshop.order.message.SeckillOrderMessage;
import com.snapshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单 MQ 消费者
 * <p>
 * 注意：handleSeckillOrder 统一由 @Transactional 包裹整个处理流程。
 * 不再通过自调用触发 processSeckillOrder，避免代理失效导致事务不生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final OrderService orderService;
    private final SeckillOrderMapper seckillOrderMapper;
    private final MqMessageLogMapper mqMessageLogMapper;
    private final InventoryFeignClient inventoryFeignClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 秒杀结果 Redis 键前缀 */
    private static final String SECKILL_RESULT_PREFIX = "seckill:result:";
    /** 秒杀结果 TTL（秒） */
    private static final long SECKILL_RESULT_TTL = 86400L;
    /** 消费状态：处理中 */
    private static final String STATUS_PROCESSING = "PROCESSING";
    /** 消费状态：成功 */
    private static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * 监听秒杀订单队列，手动确认模式
     * <p>
     * 整个处理流程在一个数据库事务中完成：
     * 消费日志写入 → 库存扣减 → 订单创建 → 结果写入 Redis → 消息确认。
     * 任何步骤失败都会回滚事务并 nack 消息，由 RabbitMQ 重新投递。
     */
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = RabbitConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(SeckillOrderMessage seckillMessage, Message message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws Exception {
        String messageId = seckillMessage.getMessageId();
        String requestId = seckillMessage.getRequestId();
        Long userId = seckillMessage.getUserId();
        Long activityId = seckillMessage.getActivityId();
        Long skuId = seckillMessage.getSkuId();

        log.info("收到秒杀订单消息: messageId={}, requestId={}, userId={}, skuId={}, activityId={}",
                messageId, requestId, userId, skuId, activityId);

        try {
            // 1. 幂等性检查：查询 mq_message_log 表
            MqMessageLog existingLog = mqMessageLogMapper.selectOne(
                    new LambdaQueryWrapper<MqMessageLog>()
                            .eq(MqMessageLog::getMessageId, messageId)
                            .eq(MqMessageLog::getConsumerGroup, RabbitConfig.CONSUMER_GROUP));

            if (existingLog != null && STATUS_SUCCESS.equals(existingLog.getStatus())) {
                log.info("消息已成功处理，直接确认: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 业务层幂等：检查 seckill_order 表（兜底：防止上一轮事务提交后 ack 丢失的场景）
            if (checkSeckillOrderExists(userId, activityId, skuId)) {
                log.info("秒杀订单已存在（业务幂等），直接确认: messageId={}, userId={}, activityId={}, skuId={}",
                        messageId, userId, activityId, skuId);
                // 确保结果缓存存在
                writeSeckillResultSuccess(requestId, null, null);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. 写入消费日志（首次插入，重试更新）
            MqMessageLog messageLog;
            if (existingLog == null) {
                messageLog = new MqMessageLog();
                messageLog.setMessageId(messageId);
                messageLog.setBusinessKey(seckillMessage.getBusinessKey());
                messageLog.setConsumerGroup(RabbitConfig.CONSUMER_GROUP);
                messageLog.setRetryCount(0);
                messageLog.setCreatedAt(LocalDateTime.now());
                messageLog.setStatus(STATUS_PROCESSING);
                messageLog.setUpdatedAt(LocalDateTime.now());
                mqMessageLogMapper.insert(messageLog);
            } else {
                messageLog = existingLog;
                messageLog.setRetryCount((messageLog.getRetryCount() != null ? messageLog.getRetryCount() : 0) + 1);
                messageLog.setStatus(STATUS_PROCESSING);
                messageLog.setUpdatedAt(LocalDateTime.now());
                mqMessageLogMapper.updateById(messageLog);
            }

            // 4. 调用库存服务扣减库存
            InventoryDeductDTO deductDTO = new InventoryDeductDTO(skuId, seckillMessage.getQuantity(), requestId);
            deductDTO.setBusinessKey(seckillMessage.getBusinessKey());
            R<InventoryResponseDTO> deductResult = inventoryFeignClient.deductStock(deductDTO);

            if (deductResult == null || deductResult.getCode() != 0 || deductResult.getData() == null
                    || !Boolean.TRUE.equals(deductResult.getData().getSuccess())) {
                throw new BizException(ErrorCode.STOCK_DEDUCT_FAILED, "库存扣减失败");
            }

            // 5. 创建订单
            CreateOrderDTO createOrderDTO = new CreateOrderDTO();
            createOrderDTO.setUserId(userId);
            createOrderDTO.setSkuId(skuId);
            createOrderDTO.setTitle("秒杀商品");
            createOrderDTO.setPrice(seckillMessage.getSeckillPrice());
            createOrderDTO.setQuantity(seckillMessage.getQuantity());
            createOrderDTO.setActivityId(activityId);
            createOrderDTO.setRequestId(requestId);
            createOrderDTO.setOrderType("SECKILL");

            Order order = orderService.createOrder(createOrderDTO);

            // 6. 写入秒杀成功结果到 Redis
            writeSeckillResultSuccess(requestId, order.getId(), order.getOrderNo());

            // 7. 更新消费日志为成功
            messageLog.setStatus(STATUS_SUCCESS);
            messageLog.setUpdatedAt(LocalDateTime.now());
            mqMessageLogMapper.updateById(messageLog);

            // 8. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("秒杀订单处理成功: messageId={}, requestId={}, orderId={}",
                    messageId, requestId, order.getId());

        } catch (BizException e) {
            // 业务异常：标记事务回滚 + nack 消息重新入队
            log.warn("秒杀订单处理业务异常: messageId={}, errorCode={}, errorMsg={}",
                    messageId, e.getCode(), e.getMessage());
            safeNack(channel, deliveryTag);
            // 仅在"已参与"错误时保留成功状态（说明订单已在库中），其他情况写失败结果
            if (e.getCode() != ErrorCode.ALREADY_PARTICIPATED.getCode()) {
                writeSeckillResultFail(requestId, e.getMessage());
            }
            // 标记事务回滚（当前未捕获抛出，需手动标记）
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (Exception e) {
            // 系统异常：标记事务回滚 + nack 消息重新入队
            log.error("秒杀订单处理系统异常: messageId={}, requestId={}, error={}",
                    messageId, requestId, e.getMessage(), e);
            safeNack(channel, deliveryTag);
            writeSeckillResultFail(requestId, e.getMessage());
            // 标记事务回滚
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    /**
     * 检查秒杀订单是否已存在（业务幂等）
     */
    private boolean checkSeckillOrderExists(Long userId, Long activityId, Long skuId) {
        Long count = seckillOrderMapper.selectCount(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getUserId, userId)
                        .eq(SeckillOrder::getActivityId, activityId)
                        .eq(SeckillOrder::getSkuId, skuId));
        return count != null && count > 0;
    }

    /**
     * 安全 nack（忽略 IO 异常）
     */
    private void safeNack(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, true);
        } catch (IOException e) {
            log.error("消息 nack 失败: deliveryTag={}", deliveryTag, e);
        }
    }

    /**
     * 写入秒杀成功结果到 Redis
     */
    private void writeSeckillResultSuccess(String requestId, Long orderId, String orderNo) {
        if (requestId == null || requestId.isEmpty()) {
            return;
        }
        try {
            String key = SECKILL_RESULT_PREFIX + requestId;
            String value = String.format(
                    "{\"requestId\":\"%s\",\"resultStatus\":\"SUCCESS\",\"orderId\":%s,\"orderNo\":%s,\"failureReason\":null}",
                    requestId,
                    orderId != null ? orderId.toString() : "null",
                    orderNo != null ? "\"" + orderNo + "\"" : "null");
            stringRedisTemplate.opsForValue().set(key, value, SECKILL_RESULT_TTL, TimeUnit.SECONDS);
            log.info("秒杀成功结果写入 Redis 成功: requestId={}", requestId);
        } catch (Exception e) {
            log.error("秒杀成功结果写入 Redis 失败: requestId={}", requestId, e);
        }
    }

    /**
     * 写入秒杀失败结果到 Redis
     */
    private void writeSeckillResultFail(String requestId, String failureReason) {
        if (requestId == null || requestId.isEmpty()) {
            return;
        }
        try {
            String key = SECKILL_RESULT_PREFIX + requestId;
            String reason = failureReason != null ? failureReason.replace("\"", "\\\"") : "系统异常";
            String value = String.format(
                    "{\"requestId\":\"%s\",\"resultStatus\":\"FAILED\",\"orderId\":null,\"orderNo\":null,\"failureReason\":\"%s\"}",
                    requestId, reason);
            stringRedisTemplate.opsForValue().set(key, value, SECKILL_RESULT_TTL, TimeUnit.SECONDS);
            log.info("秒杀失败结果写入 Redis 成功: requestId={}, reason={}", requestId, failureReason);
        } catch (Exception e) {
            log.error("秒杀失败结果写入 Redis 失败: requestId={}", requestId, e);
        }
    }
}
