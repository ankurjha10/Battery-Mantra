package com.api.batterymantra.repository;

import com.api.batterymantra.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    void deleteByProduct_ProductId(UUID productId);
}
