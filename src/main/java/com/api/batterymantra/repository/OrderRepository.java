package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Orders;
import com.api.batterymantra.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Orders, UUID> {
    List<Orders> findByOrderStatusAndPlacedAtBefore(OrderStatus status, LocalDateTime date);
    
    List<Orders> findByCustomer_UserIdOrderByPlacedAtDesc(UUID customer);
    List<Orders> findByAssignedPartner_IdOrderByPlacedAtDesc(UUID partnerId);
    Optional<Orders> findByRazorpayOrderId(String razorpayOrderId);

    // Engineer order queries
    List<Orders> findByAssignedEngineer_IdOrderByPlacedAtDesc(UUID engineerId);
    List<Orders> findByAssignedEngineer_IdAndOrderStatusInOrderByPlacedAtDesc(UUID engineerId, List<OrderStatus> statuses);

    // Partner status-filtered queries
    List<Orders> findByAssignedPartner_IdAndOrderStatusInOrderByPlacedAtDesc(UUID partnerId, List<OrderStatus> statuses);

    // Dashboard count queries
    long countByOrderStatus(OrderStatus status);
    long countByAssignedPartner_IdAndOrderStatus(UUID partnerId, OrderStatus status);
    long countByAssignedEngineer_IdAndOrderStatus(UUID engineerId, OrderStatus status);
    long countByAssignedPartnerIsNull();
}
