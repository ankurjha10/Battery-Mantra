package com.api.batterymantra.controller;

import com.api.batterymantra.dto.product.ProductListResponse;
import com.api.batterymantra.dto.product.WishlistCheckResponse;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductListResponse>> getUserWishlist(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ProductListResponse> wishlist = wishlistService.getUserWishlist(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(wishlist);
    }

    @PostMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> addProductToWishlist(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID productId) {
        wishlistService.addProductToWishlist(userPrincipal.getUser().getUserId(), productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeProductFromWishlist(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID productId) {
        wishlistService.removeProductFromWishlist(userPrincipal.getUser().getUserId(), productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WishlistCheckResponse> checkWishlist(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID productId) {
        WishlistCheckResponse response = wishlistService.checkWishlist(userPrincipal.getUser().getUserId(), productId);
        return ResponseEntity.ok(response);
    }
}
