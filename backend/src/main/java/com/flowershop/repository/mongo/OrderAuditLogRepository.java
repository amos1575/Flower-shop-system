package com.flowershop.repository.mongo;

import com.flowershop.document.OrderAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderAuditLogRepository extends MongoRepository<OrderAuditLog, String> {
    List<OrderAuditLog> findByOrderIdOrderByTimestampDesc(Long orderId);
}
