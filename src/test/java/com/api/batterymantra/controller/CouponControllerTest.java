package com.api.batterymantra.controller;

import com.api.batterymantra.dto.coupon.ApplyCouponRequest;
import com.api.batterymantra.dto.coupon.ApplyCouponResponse;
import com.api.batterymantra.dto.coupon.CouponResponse;
import com.api.batterymantra.entity.enums.DiscountType;
import com.api.batterymantra.enums.CouponTargetType;
import com.api.batterymantra.service.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(couponController).build();
    }

    @Test
    @DisplayName("GET /api/coupons/active returns 200 OK with active public coupons")
    void testGetActivePublicCoupons() throws Exception {
        UUID couponId = UUID.randomUUID();
        CouponResponse coupon = CouponResponse.builder()
                .couponId(couponId.toString())
                .code("EXIDE200")
                .discountType(DiscountType.FLAT)
                .discountValue(200.0)
                .targetType(CouponTargetType.BRANDS)
                .targetBrandIds(List.of("brand-exide"))
                .targetBrandNames(List.of("Exide"))
                .displayOnPdp(true)
                .pdpHighlightText("Get ₹200 off on all Exide Batteries")
                .build();

        when(couponService.getActivePublicCoupons()).thenReturn(List.of(coupon));

        mockMvc.perform(get("/api/coupons/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("EXIDE200"))
                .andExpect(jsonPath("$[0].discountType").value("FLAT"))
                .andExpect(jsonPath("$[0].discountValue").value(200.0))
                .andExpect(jsonPath("$[0].targetType").value("BRANDS"))
                .andExpect(jsonPath("$[0].targetBrandNames[0]").value("Exide"))
                .andExpect(jsonPath("$[0].displayOnPdp").value(true))
                .andExpect(jsonPath("$[0].pdpHighlightText").value("Get ₹200 off on all Exide Batteries"));
    }

    @Test
    @DisplayName("POST /api/coupons/apply returns 200 OK with valid apply response")
    void testApplyCoupon_Success() throws Exception {
        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("EXIDE200")
                .cartTotal(5400.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-uuid-1")
                                .categoryId("cat-uuid-car")
                                .brandId("brand-uuid-exide")
                                .productPrice(5400.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse response = ApplyCouponResponse.builder()
                .couponId(UUID.randomUUID().toString())
                .code("EXIDE200")
                .discountType(DiscountType.FLAT)
                .discountValue(200.0)
                .discountAmount(200.0)
                .isValid(true)
                .message("Coupon applied successfully")
                .finalTotal(5200.0)
                .build();

        when(couponService.applyCoupon(any(ApplyCouponRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/coupons/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXIDE200"))
                .andExpect(jsonPath("$.discountType").value("FLAT"))
                .andExpect(jsonPath("$.discountAmount").value(200.0))
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.message").value("Coupon applied successfully"))
                .andExpect(jsonPath("$.finalTotal").value(5200.0));
    }

    @Test
    @DisplayName("POST /api/coupons/apply returns 200 OK with isValid: false for non-matching targeting")
    void testApplyCoupon_TargetMismatch() throws Exception {
        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("EXIDE200")
                .cartTotal(3000.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-uuid-amaron")
                                .brandId("brand-uuid-amaron")
                                .productPrice(3000.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse response = ApplyCouponResponse.builder()
                .code("EXIDE200")
                .isValid(false)
                .discountAmount(0.0)
                .message("Coupon is only valid for eligible brands")
                .finalTotal(3000.0)
                .build();

        when(couponService.applyCoupon(any(ApplyCouponRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/coupons/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXIDE200"))
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.discountAmount").value(0.0))
                .andExpect(jsonPath("$.message").value("Coupon is only valid for eligible brands"));
    }
}

