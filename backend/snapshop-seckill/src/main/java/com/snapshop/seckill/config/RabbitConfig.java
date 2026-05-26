package com.snapshop.seckill.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：交换机、队列、绑定、发布确认
 */
@Slf4j
@Configuration
public class RabbitConfig {

    /** 秒杀下单交换机 */
    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";

    /** 秒杀下单队列 */
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";

    /** 秒杀下单路由键 */
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order.create";

    /**
     * 秒杀下单 Direct 交换机（持久化）
     */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE, true, false);
    }

    /**
     * 秒杀下单队列（持久化）
     */
    @Bean
    public Queue seckillOrderQueue() {
        return new Queue(SECKILL_ORDER_QUEUE, true);
    }

    /**
     * 绑定：seckill.order.exchange → seckill.order.queue，路由键 seckill.order.create
     */
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(SECKILL_ORDER_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate：JSON 序列化、发布确认回调、退回回调
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        rabbitTemplate.setMandatory(true);

        // 发布确认回调：消息到达交换机时触发
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null) {
                return;
            }
            if (ack) {
                log.info("消息发送确认成功，消息ID：{}", correlationData.getId());
            } else {
                log.error("消息发送到交换机失败，消息ID：{}，原因：{}", correlationData.getId(), cause);
            }
        });

        // 退回回调：消息无法路由到队列时触发
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由失败，消息：{}，回复码：{}，回复文本：{}，交换机：{}，路由键：{}",
                    new String(returned.getMessage().getBody()),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey());
        });

        return rabbitTemplate;
    }

    /**
     * 确保 RabbitTemplate 设置 ConfirmCallback 在 bean 初始化后
     */
    @PostConstruct
    public void init() {
        log.info("RabbitMQ 秒杀下单配置初始化完成：exchange={}, queue={}, routingKey={}",
                SECKILL_ORDER_EXCHANGE, SECKILL_ORDER_QUEUE, SECKILL_ORDER_ROUTING_KEY);
    }
}
