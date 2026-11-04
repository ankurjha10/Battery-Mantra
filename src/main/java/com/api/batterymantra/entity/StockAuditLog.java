package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stock_audit_logs", indexes = {
    @Index(name = "idx_stock_audit_product", columnList = "product_id"),
    @Index(name = "idx_stock_audit_order", columnList = "order_id")
})
public class StockAuditLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "change_type", length = 50, nullable = false)
    private String changeType; // 'ORDER_DEDUCT', 'ORDER_RESTORE', 'MANUAL_ADJUSTMENT', 'BULK_UPDATE'

    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(name = "previous_stock", nullable = false)
    private int previousStock;

    @Column(name = "new_stock", nullable = false)
    private int newStock;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

