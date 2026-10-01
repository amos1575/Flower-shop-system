package com.flowershop.controller;

import com.flowershop.dto.CreateOrderRequest;
import com.flowershop.dto.OrderAuditLogResponse;
import com.flowershop.dto.OrderResponse;
import com.flowershop.dto.OrderStatusUpdateRequest;
import com.flowershop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(authentication.getName(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(authentication.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.getOrderById(id, authentication.getName(), isAdmin));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> listAllOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.listAllOrders(pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody OrderStatusUpdateRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus(), authentication.getName()));
    }

    @GetMapping("/{id}/audit-log")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderAuditLogResponse>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getAuditLog(id));
    }
}
