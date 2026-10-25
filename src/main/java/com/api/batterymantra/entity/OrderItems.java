package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "order_items")
public class OrderItems {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(1)
    private int quantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @PositiveOrZero
    @Column(nullable = false)
    private BigDecimal priceAtPurchase;

    @Column(name = "exchange_old_battery", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean exchangeOldBattery = false;

    @Column(name = "exchange_discount", columnDefinition = "numeric(38,2) default 0")
    private BigDecimal exchangeDiscount;
}
