package com.snapshop.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：支付事件交换机、队列、绑定
 */
@Configuration
public class RabbitConfig {

    /** 支付事件交换机 */
    public static final String PAYMENT_EVENT_EXCHANGE = "payment.event.exchange";

    /** 支付成功队列 */
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";

    /** 支付成功路由键 */
    public static final String PAYMENT_SUCCESS_KEY = "payment.success";

    /**
     * 声明支付事件 Direct 交换机（持久化、非自动删除）
     */
    @Bean
    public DirectExchange paymentEventExchange() {
        return new DirectExchange(PAYMENT_EVENT_EXCHANGE, true, false);
    }

    /**
     * 声明支付成功队列（持久化）
     */
    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(PAYMENT_SUCCESS_QUEUE, true);
    }

    /**
     * 绑定：支付成功队列 → 支付事件交换机，路由键 payment.success
     */
    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(paymentEventExchange())
                .with(PAYMENT_SUCCESS_KEY);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
