package com.flowershop.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class InventoryReportResponse {
    private long totalActiveFlowers;
    private long totalStockUnits;
    private BigDecimal totalStockValue;
    private int lowStockThreshold;
    private List<LowStockFlowerResponse> lowStockFlowers;
}
