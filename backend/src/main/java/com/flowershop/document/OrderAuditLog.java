package com.flowershop.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Append-only audit trail of order lifecycle events, kept in MongoDB rather than
 * Postgres: it's write-heavy, schema-light, and queried independently of the
 * relational order data (never joined against it), which is exactly the kind of
 * data polyglot persistence is for.
 */
@Document(collection = "order_audit_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAuditLog {

    @Id
    private String id;

    @Indexed
    private Long orderId;

    private String previousStatus;
    private String newStatus;
    private String changedByEmail;
    private String changedByRole;
    private String note;
    private LocalDateTime timestamp;
}
