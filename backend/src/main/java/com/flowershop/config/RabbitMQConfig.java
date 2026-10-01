package com.flowershop.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A topic exchange carrying order lifecycle events to anything that wants to react
 * to them asynchronously. Today that's just the notification queue (simulated
 * email/SMS below), but the exchange/routing-key split means a future consumer
 * (analytics, a real SMS gateway, etc.) can bind its own queue without touching
 * the producer in OrderService.
 */
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATIONS_EXCHANGE = "flowershop.notifications";
    public static final String NOTIFICATIONS_EMAIL_QUEUE = "flowershop.notifications.email";
    public static final String NOTIFICATIONS_ROUTING_PATTERN = "notification.#";

    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }

    @Bean
    public Queue notificationsEmailQueue() {
        return new Queue(NOTIFICATIONS_EMAIL_QUEUE, true);
    }

    @Bean
    public Binding notificationsEmailBinding(Queue notificationsEmailQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsEmailQueue)
                .to(notificationsExchange)
                .with(NOTIFICATIONS_ROUTING_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
