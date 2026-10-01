package com.flowershop.repository;

import com.flowershop.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrderId(Long orderId);
    Page<Delivery> findByDeliveryPersonId(Long deliveryPersonId, Pageable pageable);
}
