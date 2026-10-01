package com.flowershop.repository;

import com.flowershop.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrderId(Long orderId);

    @Query("""
            SELECT COUNT(od) > 0 FROM OrderDetail od
            WHERE od.flower.id = :flowerId
            AND od.order.customer.id = :customerId
            AND od.order.status = 'DELIVERED'
            """)
    boolean existsDeliveredPurchase(@Param("flowerId") Long flowerId, @Param("customerId") Long customerId);
}
