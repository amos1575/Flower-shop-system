package com.flowershop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignDeliveryRequest {

    @NotNull
    private Long deliveryPersonId;
}
