package com.flowershop.dto;

import com.flowershop.entity.Delivery;
import com.flowershop.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeliveryResponse {
    private Long id;
    private Long orderId;
    private Long deliveryPersonId;
    private String deliveryPersonName;
    private DeliveryStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;
    private String notes;

    public static DeliveryResponse from(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrder().getId())
                .deliveryPersonId(delivery.getDeliveryPerson() != null ? delivery.getDeliveryPerson().getId() : null)
                .deliveryPersonName(delivery.getDeliveryPerson() != null ? delivery.getDeliveryPerson().getFullName() : null)
                .status(delivery.getStatus())
                .assignedAt(delivery.getAssignedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .notes(delivery.getNotes())
                .build();
    }
}
