package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    List<Wishlist> findByUserUserId(UUID userId);
    Optional<Wishlist> findByUserUserIdAndProductProductId(UUID userId, UUID productId);
    boolean existsByUserUserIdAndProductProductId(UUID userId, UUID productId);
}
