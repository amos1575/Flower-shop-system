package com.flowershop.dto;

import com.flowershop.document.OrderAuditLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderAuditLogResponse {
    private String id;
    private Long orderId;
    private String previousStatus;
    private String newStatus;
    private String changedByEmail;
    private String changedByRole;
    private String note;
    private LocalDateTime timestamp;

    public static OrderAuditLogResponse from(OrderAuditLog log) {
        return OrderAuditLogResponse.builder()
                .id(log.getId())
                .orderId(log.getOrderId())
                .previousStatus(log.getPreviousStatus())
                .newStatus(log.getNewStatus())
                .changedByEmail(log.getChangedByEmail())
                .changedByRole(log.getChangedByRole())
                .note(log.getNote())
                .timestamp(log.getTimestamp())
                .build();
    }
}
