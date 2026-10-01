package com.flowershop.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.flowershop.config.RabbitMQConfig.NOTIFICATIONS_EXCHANGE;

/**
 * Publishes order-lifecycle events to RabbitMQ. Called from OrderService; kept
 * best-effort (caught, logged, never rethrown) so a broker outage degrades to
 * "no notification sent" rather than breaking order placement/status updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderPlaced(Long orderId, String recipientEmail, String customerName) {
        publish("order.placed", orderId, recipientEmail,
                "Your Flower Shop order #" + orderId + " was placed",
                "Hi " + customerName + ", we've received your order #" + orderId
                        + " and will confirm it shortly.");
    }

    public void publishOrderStatusChanged(Long orderId, String recipientEmail, String newStatus) {
        publish("order.status-changed", orderId, recipientEmail,
                "Your Flower Shop order #" + orderId + " is now " + newStatus,
                "Your order #" + orderId + " status changed to " + newStatus + ".");
    }

    private void publish(String routingSuffix, Long orderId, String recipientEmail, String subject, String message) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .type(routingSuffix)
                    .orderId(orderId)
                    .recipientEmail(recipientEmail)
                    .subject(subject)
                    .message(message)
                    .occurredAt(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(NOTIFICATIONS_EXCHANGE, "notification." + routingSuffix, event);
        } catch (Exception e) {
            log.warn("Could not publish notification event '{}' for order {} to RabbitMQ: {}",
                    routingSuffix, orderId, e.getMessage());
        }
    }
}
