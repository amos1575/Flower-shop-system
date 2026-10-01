package com.flowershop.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message payload published to the notifications exchange. Kept as a plain,
 * Jackson-serializable POJO (not an internal entity) since it crosses a process
 * boundary once a real consumer lives in its own service.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private String type;
    private Long orderId;
    private String recipientEmail;
    private String subject;
    private String message;
    private LocalDateTime occurredAt;
}
