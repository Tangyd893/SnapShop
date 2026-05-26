package com.snapshop.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 */
@Configuration
public class RabbitConfig {

    /** 秒杀订单交换机 */
    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";

    /** 秒杀订单队列 */
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";

    /** 秒杀订单路由键 */
    public static final String SECKILL_ORDER_KEY = "seckill.order.create";

    /** 消费者组标识 */
    public static final String CONSUMER_GROUP = "snapshop-order";

    /**
     * 声明秒杀订单交换机（持久化、非自动删除）
     */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE, true, false);
    }

    /**
     * 声明秒杀订单队列（持久化）
     */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).build();
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(seckillOrderExchange()).with(SECKILL_ORDER_KEY);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
