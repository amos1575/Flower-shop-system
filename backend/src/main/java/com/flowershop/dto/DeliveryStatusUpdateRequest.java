package com.flowershop.dto;

import com.flowershop.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryStatusUpdateRequest {

    @NotNull
    private DeliveryStatus status;

    private String notes;
}
