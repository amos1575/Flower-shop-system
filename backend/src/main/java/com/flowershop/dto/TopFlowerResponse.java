package com.flowershop.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TopFlowerResponse {
    private Long flowerId;
    private String flowerName;
    private int quantitySold;
    private BigDecimal revenue;
}
