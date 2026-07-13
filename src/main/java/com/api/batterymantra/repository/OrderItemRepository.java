package com.api.batterymantra.repository;

import com.api.batterymantra.entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItems, UUID> {

    boolean existsByProduct_ProductId(UUID productId);
}
