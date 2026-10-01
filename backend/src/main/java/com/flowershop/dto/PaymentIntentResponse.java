package com.flowershop.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentIntentResponse {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String clientSecret;
}
