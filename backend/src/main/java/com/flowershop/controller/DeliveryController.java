package com.flowershop.controller;

import com.flowershop.dto.AssignDeliveryRequest;
import com.flowershop.dto.DeliveryResponse;
import com.flowershop.dto.DeliveryStatusUpdateRequest;
import com.flowershop.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryResponse> assign(@PathVariable Long id,
                                                    @Valid @RequestBody AssignDeliveryRequest request) {
        return ResponseEntity.ok(deliveryService.assign(id, request.getDeliveryPersonId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<DeliveryResponse> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody DeliveryStatusUpdateRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(deliveryService.updateStatus(
                id, authentication.getName(), request.getStatus(), request.getNotes()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<Page<DeliveryResponse>> getMyDeliveries(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(authentication.getName(), pageable));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<DeliveryResponse> getByOrderId(@PathVariable Long orderId, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        return ResponseEntity.ok(deliveryService.getByOrderId(orderId, authentication.getName(), isAdmin));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DeliveryResponse>> listAll(Pageable pageable) {
        return ResponseEntity.ok(deliveryService.listAll(pageable));
    }
}
