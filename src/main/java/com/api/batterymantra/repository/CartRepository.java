package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    @Query("SELECT c FROM Cart c WHERE c.customer.userId = :userId")
    Optional<Cart> findByUserId(UUID userId);

}
