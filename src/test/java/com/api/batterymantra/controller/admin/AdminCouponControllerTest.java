package com.api.batterymantra.controller.admin;

import com.api.batterymantra.dto.coupon.CouponRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminCouponControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private AdminCouponController adminCouponController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(adminCouponController).build();
    }

    @Test
    @DisplayName("POST /api/admin/coupons creates coupon with targeting and PDP fields")
    void testCreateCoupon() throws Exception {
        UUID couponId = UUID.randomUUID();
        CouponRequest request = CouponRequest.builder()
                .code("TARGETCAT10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10.0)
                .minOrderValue(1000.0)
                .targetType(CouponTargetType.CATEGORIES)
                .targetCategoryIds(List.of("cat-inverter"))
                .targetCategoryNames(List.of("Inverter Batteries"))
                .displayOnPdp(true)
                .pdpHighlightText("10% off on all Inverter Batteries")
                .build();

        CouponResponse response = CouponResponse.builder()
                .couponId(couponId.toString())
                .code("TARGETCAT10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10.0)
                .minOrderValue(1000.0)
                .targetType(CouponTargetType.CATEGORIES)
                .targetCategoryIds(List.of("cat-inverter"))
                .targetCategoryNames(List.of("Inverter Batteries"))
                .displayOnPdp(true)
                .pdpHighlightText("10% off on all Inverter Batteries")
                .build();

        when(couponService.createCoupon(any(CouponRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TARGETCAT10"))
                .andExpect(jsonPath("$.targetType").value("CATEGORIES"))
                .andExpect(jsonPath("$.targetCategoryIds[0]").value("cat-inverter"))
                .andExpect(jsonPath("$.targetCategoryNames[0]").value("Inverter Batteries"))
                .andExpect(jsonPath("$.displayOnPdp").value(true))
                .andExpect(jsonPath("$.pdpHighlightText").value("10% off on all Inverter Batteries"));
    }

    @Test
    @DisplayName("PUT /api/admin/coupons/{id} updates targeting fields")
    void testUpdateCoupon() throws Exception {
        UUID couponId = UUID.randomUUID();
        CouponRequest request = CouponRequest.builder()
                .code("TARGETCAT10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(15.0)
                .targetType(CouponTargetType.CATEGORIES)
                .targetCategoryIds(List.of("cat-inverter", "cat-solar"))
                .build();

        CouponResponse response = CouponResponse.builder()
                .couponId(couponId.toString())
                .code("TARGETCAT10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(15.0)
                .targetType(CouponTargetType.CATEGORIES)
                .targetCategoryIds(List.of("cat-inverter", "cat-solar"))
                .build();

        when(couponService.updateCoupon(eq(couponId), any(CouponRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/coupons/" + couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountValue").value(15.0))
                .andExpect(jsonPath("$.targetCategoryIds.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/admin/coupons returns all coupons")
    void testGetAllCoupons() throws Exception {
        CouponResponse c1 = CouponResponse.builder()
                .couponId(UUID.randomUUID().toString())
                .code("C1")
                .discountType(DiscountType.FLAT)
                .discountValue(50.0)
                .build();

        when(couponService.getAllCoupons()).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("C1"));
    }

    @Test
    @DisplayName("DELETE /api/admin/coupons/{id} deletes coupon")
    void testDeleteCoupon() throws Exception {
        UUID couponId = UUID.randomUUID();
        doNothing().when(couponService).deleteCoupon(couponId);

        mockMvc.perform(delete("/api/admin/coupons/" + couponId))
                .andExpect(status().isNoContent());

        verify(couponService, times(1)).deleteCoupon(couponId);
    }
}

