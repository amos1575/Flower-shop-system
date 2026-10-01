package com.flowershop.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.flowershop.config.RabbitMQConfig.NOTIFICATIONS_EMAIL_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationSender notificationSender;

    @RabbitListener(queues = NOTIFICATIONS_EMAIL_QUEUE)
    public void onNotification(NotificationEvent event) {
        log.debug("Consumed notification event: {}", event.getType());
        notificationSender.sendEmail(event);
    }
}
