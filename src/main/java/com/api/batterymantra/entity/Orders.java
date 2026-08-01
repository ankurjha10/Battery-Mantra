package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.PaymentMethod;
import java.time.LocalDate;
import com.api.batterymantra.entity.enums.DeliveryMethod;

import com.api.batterymantra.entity.enums.OrderStatus;
import com.api.batterymantra.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User customer;

    @PositiveOrZero
    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address shippingAddress;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @CreationTimestamp
    private LocalDateTime placedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method")
    private DeliveryMethod deliveryMethod;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "exchange_discount")
    private BigDecimal exchangeDiscount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItems> orderItems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_partner_id")
    private PartnerProfile assignedPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_engineer_id")
    private EngineerProfile assignedEngineer;

    @Column(name = "delivery_security_code")
    private String deliverySecurityCode;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

}
