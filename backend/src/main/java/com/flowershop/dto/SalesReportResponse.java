package com.flowershop.dto;

import com.flowershop.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SalesReportResponse {
    private LocalDate from;
    private LocalDate to;
    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Map<OrderStatus, Long> ordersByStatus;
    private List<TopFlowerResponse> topFlowers;
}
