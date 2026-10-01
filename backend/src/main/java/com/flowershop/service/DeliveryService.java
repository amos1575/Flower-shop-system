package com.flowershop.service;

import com.flowershop.dto.DeliveryResponse;
import com.flowershop.entity.Delivery;
import com.flowershop.entity.DeliveryStatus;
import com.flowershop.entity.Order;
import com.flowershop.entity.OrderStatus;
import com.flowershop.entity.Role;
import com.flowershop.entity.User;
import com.flowershop.exception.ApiException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.repository.DeliveryRepository;
import com.flowershop.repository.OrderRepository;
import com.flowershop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeliveryResponse assign(Long deliveryId, Long deliveryPersonId) {
        Delivery delivery = findOrThrow(deliveryId);

        User deliveryPerson = userRepository.findById(deliveryPersonId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + deliveryPersonId));
        if (deliveryPerson.getRole() != Role.DELIVERY) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "User '" + deliveryPerson.getEmail() + "' is not a delivery person");
        }

        delivery.setDeliveryPerson(deliveryPerson);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());

        return DeliveryResponse.from(deliveryRepository.save(delivery));
    }

    @Transactional
    public DeliveryResponse updateStatus(Long deliveryId, String requesterEmail, DeliveryStatus status, String notes) {
        Delivery delivery = findOrThrow(deliveryId);

        if (delivery.getDeliveryPerson() == null
                || !delivery.getDeliveryPerson().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("You are not assigned to this delivery");
        }

        delivery.setStatus(status);
        if (notes != null) {
            delivery.setNotes(notes);
        }

        Order order = delivery.getOrder();
        if (status == DeliveryStatus.OUT_FOR_DELIVERY
                && (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PROCESSING)) {
            order.setStatus(OrderStatus.SHIPPED);
            orderRepository.save(order);
        } else if (status == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        }

        return DeliveryResponse.from(deliveryRepository.save(delivery));
    }

    @Transactional(readOnly = true)
    public Page<DeliveryResponse> getMyDeliveries(String requesterEmail, Pageable pageable) {
        User deliveryPerson = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterEmail));
        return deliveryRepository.findByDeliveryPersonId(deliveryPerson.getId(), pageable).map(DeliveryResponse::from);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrderId(Long orderId, String requesterEmail, boolean isAdmin) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No delivery found for order: " + orderId));

        if (!isAdmin && !delivery.getOrder().getCustomer().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("You do not have permission to view this delivery");
        }

        return DeliveryResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryResponse> listAll(Pageable pageable) {
        return deliveryRepository.findAll(pageable).map(DeliveryResponse::from);
    }

    private Delivery findOrThrow(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + deliveryId));
    }
}
