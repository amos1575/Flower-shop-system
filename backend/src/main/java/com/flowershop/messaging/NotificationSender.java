package com.flowershop.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simulated delivery channel for notifications consumed off the RabbitMQ queue.
 * Logs what would be sent instead of calling a real provider — exactly like this
 * project's Stripe keys ship as placeholders (see application.yml / StripeConfig):
 * the integration point is real and wired end-to-end, swapping in a real SMTP
 * client (JavaMailSender) or SMS gateway (e.g. Twilio) here is a drop-in change
 * that doesn't touch the publisher or the queue topology.
 */
@Slf4j
@Component
public class NotificationSender {

    public void sendEmail(NotificationEvent event) {
        log.info("[simulated email] to={} subject=\"{}\" body=\"{}\"",
                event.getRecipientEmail(), event.getSubject(), event.getMessage());
    }
}
