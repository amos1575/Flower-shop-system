package com.flowershop.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LowStockFlowerResponse {
    private Long flowerId;
    private String name;
    private int stockQuantity;
}
