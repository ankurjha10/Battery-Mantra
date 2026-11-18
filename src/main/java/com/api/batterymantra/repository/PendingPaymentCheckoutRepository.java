package com.api.batterymantra.repository;

import com.api.batterymantra.entity.PendingPaymentCheckout;
import com.api.batterymantra.entity.enums.PaymentCheckoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PendingPaymentCheckoutRepository extends JpaRepository<PendingPaymentCheckout, UUID> {

    Optional<PendingPaymentCheckout> findByRazorpayOrderId(String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PendingPaymentCheckout p WHERE p.razorpayOrderId = :razorpayOrderId")
    Optional<PendingPaymentCheckout> findByRazorpayOrderIdForUpdate(@Param("razorpayOrderId") String razorpayOrderId);

    List<PendingPaymentCheckout> findByStatusAndExpiresAtBefore(PaymentCheckoutStatus status, LocalDateTime cutoff);
}
