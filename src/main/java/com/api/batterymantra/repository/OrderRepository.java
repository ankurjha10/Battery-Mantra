package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Orders, UUID> {
    List<Orders> findByCustomer_UserIdOrderByPlacedAtDesc(UUID customer);
    List<Orders> findByAssignedPartner_IdOrderByPlacedAtDesc(UUID partnerId);
}
