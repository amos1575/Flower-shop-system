package com.flowershop.service;

import com.flowershop.dto.InventoryReportResponse;
import com.flowershop.dto.LowStockFlowerResponse;
import com.flowershop.dto.SalesReportResponse;
import com.flowershop.dto.TopFlowerResponse;
import com.flowershop.entity.Flower;
import com.flowershop.entity.Order;
import com.flowershop.entity.OrderStatus;
import com.flowershop.repository.FlowerRepository;
import com.flowershop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int TOP_FLOWERS_LIMIT = 5;

    private final OrderRepository orderRepository;
    private final FlowerRepository flowerRepository;

    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        List<Order> revenueOrders = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .toList();

        BigDecimal totalRevenue = revenueOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = revenueOrders.size();
        BigDecimal averageOrderValue = totalOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        Map<OrderStatus, Long> ordersByStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        record FlowerSale(Long flowerId, String flowerName, int quantity, BigDecimal revenue) {
        }

        List<FlowerSale> sales = revenueOrders.stream()
                .flatMap(o -> o.getOrderDetails().stream())
                .map(od -> new FlowerSale(od.getFlower().getId(), od.getFlower().getName(), od.getQuantity(),
                        od.getUnitPrice().multiply(BigDecimal.valueOf(od.getQuantity()))))
                .toList();

        List<TopFlowerResponse> topFlowers = sales.stream()
                .collect(Collectors.groupingBy(FlowerSale::flowerId))
                .values().stream()
                .map(group -> TopFlowerResponse.builder()
                        .flowerId(group.get(0).flowerId())
                        .flowerName(group.get(0).flowerName())
                        .quantitySold(group.stream().mapToInt(FlowerSale::quantity).sum())
                        .revenue(group.stream().map(FlowerSale::revenue).reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .sorted(Comparator.comparingInt(TopFlowerResponse::getQuantitySold).reversed())
                .limit(TOP_FLOWERS_LIMIT)
                .toList();

        return SalesReportResponse.builder()
                .from(from)
                .to(to)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .ordersByStatus(ordersByStatus)
                .topFlowers(topFlowers)
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse getInventoryReport() {
        List<Flower> activeFlowers = flowerRepository.findAll().stream()
                .filter(Flower::isActive)
                .toList();

        long totalStockUnits = activeFlowers.stream().mapToLong(Flower::getStockQuantity).sum();
        BigDecimal totalStockValue = activeFlowers.stream()
                .map(f -> f.getPrice().multiply(BigDecimal.valueOf(f.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LowStockFlowerResponse> lowStockFlowers = activeFlowers.stream()
                .filter(f -> f.getStockQuantity() < LOW_STOCK_THRESHOLD)
                .sorted(Comparator.comparingInt(Flower::getStockQuantity))
                .map(f -> LowStockFlowerResponse.builder()
                        .flowerId(f.getId())
                        .name(f.getName())
                        .stockQuantity(f.getStockQuantity())
                        .build())
                .toList();

        return InventoryReportResponse.builder()
                .totalActiveFlowers(activeFlowers.size())
                .totalStockUnits(totalStockUnits)
                .totalStockValue(totalStockValue)
                .lowStockThreshold(LOW_STOCK_THRESHOLD)
                .lowStockFlowers(lowStockFlowers)
                .build();
    }
}
