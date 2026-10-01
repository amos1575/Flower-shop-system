package com.flowershop.service;

import com.flowershop.document.OrderAuditLog;
import com.flowershop.dto.CreateOrderRequest;
import com.flowershop.dto.OrderAuditLogResponse;
import com.flowershop.dto.OrderItemRequest;
import com.flowershop.dto.OrderResponse;
import com.flowershop.entity.*;
import com.flowershop.exception.ApiException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.messaging.NotificationPublisher;
import com.flowershop.repository.DeliveryRepository;
import com.flowershop.repository.FlowerRepository;
import com.flowershop.repository.OrderRepository;
import com.flowershop.repository.UserRepository;
import com.flowershop.repository.mongo.OrderAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final FlowerRepository flowerRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationPublisher notificationPublisher;
    private final OrderAuditLogRepository orderAuditLogRepository;

    @Transactional
    public OrderResponse createOrder(String customerEmail, CreateOrderRequest request) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + customerEmail));

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .deliveryAddress(request.getDeliveryAddress())
                .build();

        List<OrderDetail> details = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest item : request.getItems()) {
            Flower flower = flowerRepository.findById(item.getFlowerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Flower not found: " + item.getFlowerId()));

            if (!flower.isActive()) {
                throw new ApiException(HttpStatus.CONFLICT, "Flower '" + flower.getName() + "' is no longer available");
            }
            if (flower.getStockQuantity() < item.getQuantity()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Insufficient stock for '" + flower.getName() + "': requested " + item.getQuantity()
                                + ", available " + flower.getStockQuantity());
            }

            flower.setStockQuantity(flower.getStockQuantity() - item.getQuantity());
            flowerRepository.save(flower);

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .flower(flower)
                    .quantity(item.getQuantity())
                    .unitPrice(flower.getPrice())
                    .build();
            details.add(detail);

            total = total.add(flower.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setTotalAmount(total);
        order.setOrderDetails(details);

        Order savedOrder = orderRepository.save(order);

        Delivery delivery = Delivery.builder()
                .order(savedOrder)
                .status(DeliveryStatus.UNASSIGNED)
                .build();
        deliveryRepository.save(delivery);

        notificationPublisher.publishOrderPlaced(savedOrder.getId(), customer.getEmail(), customer.getFullName());
        logAuditEvent(savedOrder.getId(), null, OrderStatus.PENDING.name(),
                customer.getEmail(), customer.getRole().name(), "Order placed");

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String customerEmail, Pageable pageable) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + customerEmail));
        return orderRepository.findByCustomerId(customer.getId(), pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String requesterEmail, boolean isAdmin) {
        Order order = findOrThrow(orderId);
        if (!isAdmin && !order.getCustomer().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status, String requesterEmail) {
        Order order = findOrThrow(orderId);
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        Order saved = orderRepository.save(order);

        notificationPublisher.publishOrderStatusChanged(orderId, order.getCustomer().getEmail(), status.name());

        return OrderResponse.from(saved);
        User requester = userRepository.findByEmail(requesterEmail).orElse(null);
        logAuditEvent(orderId, previousStatus.name(), status.name(),
                requesterEmail, requester != null ? requester.getRole().name() : "ADMIN", null);

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderAuditLogResponse> getAuditLog(Long orderId) {
        findOrThrow(orderId);
        try {
            return orderAuditLogRepository.findByOrderIdOrderByTimestampDesc(orderId).stream()
                    .map(OrderAuditLogResponse::from)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Could not read order audit log for order {} from MongoDB: {}", orderId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // Best-effort: the audit trail lives in MongoDB, a separate store from the order
    // data itself, so an outage there must never fail the (Postgres-backed) order
    // transaction it's describing.
    private void logAuditEvent(Long orderId, String previousStatus, String newStatus,
                                String changedByEmail, String changedByRole, String note) {
        try {
            orderAuditLogRepository.save(OrderAuditLog.builder()
                    .orderId(orderId)
                    .previousStatus(previousStatus)
                    .newStatus(newStatus)
                    .changedByEmail(changedByEmail)
                    .changedByRole(changedByRole)
                    .note(note)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (RuntimeException e) {
            log.warn("Could not write order audit log for order {} to MongoDB: {}", orderId, e.getMessage());
        }
    }

    private Order findOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
}
