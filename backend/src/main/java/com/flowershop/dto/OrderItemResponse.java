package com.flowershop.dto;

import com.flowershop.entity.OrderDetail;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {
    private Long flowerId;
    private String flowerName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public static OrderItemResponse from(OrderDetail detail) {
        return OrderItemResponse.builder()
                .flowerId(detail.getFlower().getId())
                .flowerName(detail.getFlower().getName())
                .quantity(detail.getQuantity())
                .unitPrice(detail.getUnitPrice())
                .lineTotal(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .build();
    }
}
