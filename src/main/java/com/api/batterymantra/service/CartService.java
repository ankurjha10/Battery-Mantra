package com.api.batterymantra.service;

import java.math.BigDecimal;

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
import com.api.batterymantra.repository.CartRepository;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.UserRepository;
import com.api.batterymantra.util.ProductMapper;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart(UUID userId) {
        return getCart(userId, null);
    }

    @Transactional
    public CartResponse getCart(UUID userId, UUID cityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Cart cart = cartRepository.findByUserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().customer(user).build();
                    return cartRepository.save(newCart);
                });

        return mapToCartResponse(cart, cityId);
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
        return mapToCartResponse(cart, null);
    }

    private CartResponse mapToCartResponse(Cart cart, UUID cityId) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getCartId());
        response.setUserId(cart.getCustomer().getUserId());

        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal exchangeDiscount = BigDecimal.ZERO;

        if (cart.getCartItems() != null) {
            response.setCartItems(cart.getCartItems().stream()
                    .map(item -> mapToCartItemResponse(item, cityId))
                    .toList());

            for (CartItem item : cart.getCartItems()) {
                BigDecimal itemPrice = item.getProduct().getProductPrice();
                BigDecimal itemExchange = item.getProduct().getExchangeDiscount() != null
                        ? item.getProduct().getExchangeDiscount()
                        : BigDecimal.ZERO;

                // City-wise pricing override if cityId is provided
                if (cityId != null && item.getProduct().getCityPrices() != null) {
                    var cpOpt = item.getProduct().getCityPrices().stream()
                            .filter(cp -> cp.getCity() != null && cp.getCity().getCityId().equals(cityId))
                            .findFirst();
                    if (cpOpt.isPresent()) {
                        var cp = cpOpt.get();
                        if (cp.getPrice() != null) {
                            itemPrice = cp.getPrice();
                        }
                        if (cp.getExchangeDiscount() != null && cp.getExchangeDiscount().compareTo(BigDecimal.ZERO) > 0) {
                            itemExchange = cp.getExchangeDiscount();
                        }
                    }
                }

                subTotal = subTotal.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                if (item.isExchangeOldBattery()) {
                    exchangeDiscount = exchangeDiscount.add(itemExchange.multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        } else {
            response.setCartItems(new ArrayList<>());
        }

        response.setSubTotal(subTotal);
        response.setExchangeDiscount(exchangeDiscount);
        BigDecimal total = subTotal.subtract(exchangeDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        response.setTotalAmount(total);

        return response;
    }

    private CartItemResponse mapToCartItemResponse(CartItem cartItem) {
        return mapToCartItemResponse(cartItem, null);
    }

    private CartItemResponse mapToCartItemResponse(CartItem cartItem, UUID cityId) {
        CartItemResponse response = new CartItemResponse();
        response.setCartItemId(cartItem.getCartItemId());
        response.setQuantity(cartItem.getQuantity());
        response.setExchangeOldBattery(cartItem.isExchangeOldBattery());

        ProductListResponse productResponse = new ProductListResponse();
        Product product = cartItem.getProduct();
        productResponse.setProductId(product.getProductId());
        productResponse.setProductName(product.getProductName());

        BigDecimal itemPrice = product.getProductPrice();
        BigDecimal itemExchange = product.getExchangeDiscount() != null ? product.getExchangeDiscount() : BigDecimal.ZERO;

        // City-wise pricing override if cityId is provided
        if (cityId != null && product.getCityPrices() != null) {
            var cpOpt = product.getCityPrices().stream()
                    .filter(cp -> cp.getCity() != null && cp.getCity().getCityId().equals(cityId))
                    .findFirst();
            if (cpOpt.isPresent()) {
                var cp = cpOpt.get();
                if (cp.getPrice() != null) {
                    itemPrice = cp.getPrice();
                }
                if (cp.getExchangeDiscount() != null && cp.getExchangeDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    itemExchange = cp.getExchangeDiscount();
                }
            }
        }

        productResponse.setProductPrice(itemPrice);
        productResponse.setExchangeDiscount(itemExchange);
        productResponse.setProductImage(product.getProductImage());
        if (product.getProductCategory() != null) {
            productResponse.setProductCategory(product.getProductCategory().getCategoryName());
            productResponse.setCategoryId(product.getProductCategory().getCategoryId());
        }
        productResponse.setCategories(ProductMapper.mapCategories(product));
        productResponse.setBrandName(product.getBrand() != null ? product.getBrand().getBrandName() : null);
        productResponse.setSearchKeywords(product.getSearchKeywords());

        response.setProduct(productResponse);
        return response;
    }
}
