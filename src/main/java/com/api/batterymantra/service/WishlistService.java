package com.api.batterymantra.service;

import com.api.batterymantra.dto.product.ProductListResponse;
import com.api.batterymantra.dto.product.WishlistCheckResponse;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.Wishlist;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public List<ProductListResponse> getUserWishlist(UUID userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserUserId(userId);
        return wishlists.stream()
                .map(w -> productService.toListResponse(w.getProduct(), null))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addProductToWishlist(UUID userId, UUID productId) {
        if (wishlistRepository.existsByUserUserIdAndProductProductId(userId, productId)) {
            return; // Already in wishlist
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
        
        wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeProductFromWishlist(UUID userId, UUID productId) {
        Optional<Wishlist> wishlistOpt = wishlistRepository.findByUserUserIdAndProductProductId(userId, productId);
        wishlistOpt.ifPresent(wishlistRepository::delete);
    }

    @Transactional(readOnly = true)
    public WishlistCheckResponse checkWishlist(UUID userId, UUID productId) {
        boolean isWishlisted = wishlistRepository.existsByUserUserIdAndProductProductId(userId, productId);
        return new WishlistCheckResponse(isWishlisted);
    }
}
