package com.api.batterymantra.util;

import com.api.batterymantra.dto.order.OrderResponse;
import com.api.batterymantra.entity.Orders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperSecurityCodeTest {

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Delivery security code is returned in payload for PARTNER")
    void testSecurityCode_ReturnedForPartner() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("partner_user", "pass", List.of(new SimpleGrantedAuthority("ROLE_PARTNER")))
        );

        Orders order = Orders.builder()
                .orderId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(2500))
                .deliverySecurityCode("5821")
                .build();

        OrderResponse response = orderMapper.toOrderResponse(order);

        assertEquals("5821", response.getDeliverySecurityCode());
    }

    @Test
    @DisplayName("Delivery security code is returned in payload for ADMIN")
    void testSecurityCode_ReturnedForAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin_user", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        Orders order = Orders.builder()
                .orderId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(2500))
                .deliverySecurityCode("4321")
                .build();

        OrderResponse response = orderMapper.toOrderResponse(order);

        assertEquals("4321", response.getDeliverySecurityCode());
    }

    @Test
    @DisplayName("Delivery security code is hidden for CUSTOMER")
    void testSecurityCode_HiddenForCustomer() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer_user", "pass", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
        );

        Orders order = Orders.builder()
                .orderId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(2500))
                .deliverySecurityCode("5821")
                .build();

        OrderResponse response = orderMapper.toOrderResponse(order);

        assertNull(response.getDeliverySecurityCode(), "Delivery security code should NOT be returned to normal customers via API");
    }

    @Test
    @DisplayName("Fallback 4-digit security code is generated if entity has null code")
    void testSecurityCode_FallbackForPartnerIfNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("partner_user", "pass", List.of(new SimpleGrantedAuthority("ROLE_PARTNER")))
        );

        Orders order = Orders.builder()
                .orderId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(2500))
                .deliverySecurityCode(null)
                .build();

        OrderResponse response = orderMapper.toOrderResponse(order);

        assertNotNull(response.getDeliverySecurityCode());
        assertEquals(4, response.getDeliverySecurityCode().length());
    }
}
