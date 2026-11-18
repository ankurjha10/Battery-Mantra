package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.DeliveryMethod;
import com.api.batterymantra.entity.enums.PaymentCheckoutStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "pending_payment_checkouts",
        indexes = {
                @Index(name = "idx_pending_checkout_rzp_order", columnList = "razorpay_order_id", unique = true),
                @Index(name = "idx_pending_checkout_status_expires", columnList = "status, expires_at")
        }
)
public class PendingPaymentCheckout {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "razorpay_order_id", nullable = false, unique = true)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false)
    private DeliveryMethod deliveryMethod;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @PositiveOrZero
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "exchange_discount")
    private BigDecimal exchangeDiscount;

    @Column(name = "selected_city")
    private String selectedCity;

    @Column(name = "applied_campaign_id")
    private UUID appliedCampaignId;

    @Column(name = "campaign_name")
    private String campaignName;

    @Column(name = "campaign_discount_amount")
    @Builder.Default
    private BigDecimal campaignDiscountAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "coupon_code")
    private String couponCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_partner_id")
    private PartnerProfile assignedPartner;

    @Column(name = "items_snapshot_json", columnDefinition = "TEXT", nullable = false)
    private String itemsSnapshotJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentCheckoutStatus status = PaymentCheckoutStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_order_id")
    private UUID createdOrderId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
