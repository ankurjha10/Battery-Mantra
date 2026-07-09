package com.api.batterymantra.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.api.batterymantra.dto.cart.AddToCartRequest;
import com.api.batterymantra.dto.cart.CartItemResponse;
import com.api.batterymantra.dto.cart.CartResponse;
import com.api.batterymantra.dto.product.ProductListResponse;
import com.api.batterymantra.entity.Cart;
import com.api.batterymantra.entity.CartItem;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.User;
import com.api.batterymantra.repository.CartItemRepository;
import com.api.batterymantra.repository.CartRepository;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.UserRepository;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Cart cart = cartRepository.findByUserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().customer(user).build();
                    return cartRepository.save(newCart);
                });

        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Product product = productRepository.findById(request.productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getProductStock() < request.quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient stock for product: " + product.getProductName());
        }

        // Find or create cart
        Cart cart = cartRepository.findByUserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().customer(user).build();
                    return cartRepository.save(newCart);
                });

        // Check if product already in cart
        CartItem existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(product.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity);
        } else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity)
                    .exchangeOldBattery(request.exchangeOldBattery)
                    .build();
            cart.getCartItems().add(cartItem);
        }

        cart = cartRepository.save(cart);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItemQuantity(UUID userId, UUID cartItemId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        if (cartItem.getProduct().getProductStock() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient stock for product: " + cartItem.getProduct().getProductName());
        }

        cartItem.setQuantity(quantity);
        cartRepository.save(cart);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse removeFromCart(UUID userId, UUID cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        cart.getCartItems().remove(cartItem);
        cartRepository.save(cart);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        cart.getCartItems().clear();
        Cart updatedCart = cartRepository.save(cart);
        return mapToCartResponse(updatedCart);
    }

    private CartResponse mapToCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getCartId());
        response.setUserId(cart.getCustomer().getUserId());
        
        java.math.BigDecimal subTotal = java.math.BigDecimal.ZERO;
        java.math.BigDecimal exchangeDiscount = java.math.BigDecimal.ZERO;
        
        if (cart.getCartItems() != null) {
            response.setCartItems(cart.getCartItems().stream()
                    .map(this::mapToCartItemResponse)
                    .toList());
            
            for (CartItem item : cart.getCartItems()) {
                subTotal = subTotal.add(item.getProduct().getProductPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                if (item.isExchangeOldBattery() && item.getProduct().getExchangeDiscount() != null) {
                    exchangeDiscount = exchangeDiscount.add(item.getProduct().getExchangeDiscount().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                }
            }
        } else {
            response.setCartItems(new ArrayList<>());
        }
        
        response.setSubTotal(subTotal);
        response.setExchangeDiscount(exchangeDiscount);
        java.math.BigDecimal total = subTotal.subtract(exchangeDiscount);
        if (total.compareTo(java.math.BigDecimal.ZERO) < 0) {
            total = java.math.BigDecimal.ZERO;
        }
        response.setTotalAmount(total);
        
        return response;
    }

    private CartItemResponse mapToCartItemResponse(CartItem cartItem) {
        CartItemResponse response = new CartItemResponse();
        response.setCartItemId(cartItem.getCartItemId());
        response.setQuantity(cartItem.getQuantity());
        response.setExchangeOldBattery(cartItem.isExchangeOldBattery());

        ProductListResponse productResponse = new ProductListResponse();
        Product product = cartItem.getProduct();
        productResponse.setProductId(product.getProductId());
        productResponse.setProductName(product.getProductName());
        productResponse.setProductPrice(product.getProductPrice());
        productResponse.setExchangeDiscount(product.getExchangeDiscount());
        productResponse.setProductImage(product.getProductImage());
        productResponse.setProductCategory(product.getProductCategory().getCategoryName());
        productResponse.setBrandName(product.getBrand() != null ? product.getBrand().getBrandName() : null);

        response.setProduct(productResponse);
        return response;
    }
}
