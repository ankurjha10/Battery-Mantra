package com.api.batterymantra.service;

import com.api.batterymantra.dto.coupon.ApplyCouponRequest;
import com.api.batterymantra.dto.coupon.ApplyCouponResponse;
import com.api.batterymantra.dto.coupon.CouponRequest;
import com.api.batterymantra.dto.coupon.CouponResponse;
import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.Coupon;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.enums.DiscountType;
import com.api.batterymantra.enums.CouponTargetType;
import com.api.batterymantra.repository.CouponRepository;
import com.api.batterymantra.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon activeCoupon;
    private UUID couponId;

    @BeforeEach
    void setUp() {
        couponId = UUID.randomUUID();
        activeCoupon = Coupon.builder()
                .id(couponId)
                .code("SAVE100")
                .discountType(DiscountType.FLAT)
                .discountValue(100.0)
                .minOrderValue(500.0)
                .usageLimit(50)
                .usedCount(5)
                .isActive(true)
                .startDate(LocalDateTime.now().minusDays(2))
                .expiryDate(LocalDateTime.now().plusDays(10))
                .targetType(CouponTargetType.ALL_PRODUCTS)
                .displayOnPdp(true)
                .pdpHighlightText("Save ₹100 instantly on your order!")
                .build();
    }

    @Test
    @DisplayName("getActivePublicCoupons: Returns active public coupons with targeting & PDP metadata")
    void testGetActivePublicCoupons() {
        when(couponRepository.findActivePublicCoupons(any(LocalDateTime.class)))
                .thenReturn(List.of(activeCoupon));

        List<CouponResponse> result = couponService.getActivePublicCoupons();

        assertNotNull(result);
        assertEquals(1, result.size());
        CouponResponse res = result.get(0);
        assertEquals(couponId.toString(), res.getCouponId());
        assertEquals("SAVE100", res.getCode());
        assertEquals(CouponTargetType.ALL_PRODUCTS, res.getTargetType());
        assertTrue(res.getDisplayOnPdp());
        assertEquals("Save ₹100 instantly on your order!", res.getPdpHighlightText());
    }

    @Test
    @DisplayName("applyCoupon: Blank or null code returns invalid")
    void testApplyCoupon_BlankCode() {
        ApplyCouponResponse resNull = couponService.applyCoupon(null);
        assertFalse(resNull.getIsValid());
        assertEquals("Coupon code is required", resNull.getMessage());

        ApplyCouponResponse resBlank = couponService.applyCoupon(ApplyCouponRequest.builder().code("   ").build());
        assertFalse(resBlank.getIsValid());
        assertEquals("Coupon code is required", resBlank.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: Non-existent code returns invalid")
    void testApplyCoupon_NotFound() {
        when(couponRepository.findByCodeIgnoreCase("NONEXISTENT")).thenReturn(Optional.empty());

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("NONEXISTENT")
                .cartTotal(1000.0)
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertFalse(res.getIsValid());
        assertEquals("Invalid coupon code", res.getMessage());
        assertEquals(0.0, res.getDiscountAmount());
    }

    @Test
    @DisplayName("applyCoupon: Inactive coupon returns invalid")
    void testApplyCoupon_Inactive() {
        activeCoupon.setIsActive(false);
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponResponse res = couponService.applyCoupon(ApplyCouponRequest.builder().code("SAVE100").cartTotal(1000.0).build());
        assertFalse(res.getIsValid());
        assertEquals("Coupon is inactive", res.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: Not yet active coupon returns invalid")
    void testApplyCoupon_NotYetActive() {
        activeCoupon.setStartDate(LocalDateTime.now().plusDays(2));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponResponse res = couponService.applyCoupon(ApplyCouponRequest.builder().code("SAVE100").cartTotal(1000.0).build());
        assertFalse(res.getIsValid());
        assertEquals("Coupon is not yet active", res.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: Expired coupon returns invalid")
    void testApplyCoupon_Expired() {
        activeCoupon.setExpiryDate(LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponResponse res = couponService.applyCoupon(ApplyCouponRequest.builder().code("SAVE100").cartTotal(1000.0).build());
        assertFalse(res.getIsValid());
        assertEquals("Coupon has expired", res.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: Usage limit reached returns invalid")
    void testApplyCoupon_UsageLimitExceeded() {
        activeCoupon.setUsageLimit(10);
        activeCoupon.setUsedCount(10);
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponResponse res = couponService.applyCoupon(ApplyCouponRequest.builder().code("SAVE100").cartTotal(1000.0).build());
        assertFalse(res.getIsValid());
        assertEquals("Coupon usage limit exceeded", res.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: Cart total less than minOrderValue returns invalid")
    void testApplyCoupon_MinOrderValueNotMet() {
        activeCoupon.setMinOrderValue(1500.0);
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponResponse res = couponService.applyCoupon(ApplyCouponRequest.builder().code("SAVE100").cartTotal(1200.0).build());
        assertFalse(res.getIsValid());
        assertTrue(res.getMessage().contains("Cart total must be at least 1500.0"));
    }

    @Test
    @DisplayName("applyCoupon: ALL_PRODUCTS flat discount applied successfully")
    void testApplyCoupon_AllProducts_FlatSuccess() {
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(1000.0)
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertTrue(res.getIsValid());
        assertEquals(100.0, res.getDiscountAmount());
        assertEquals(900.0, res.getFinalTotal());
        assertEquals("Coupon applied successfully", res.getMessage());
        assertEquals(couponId.toString(), res.getCouponId());
    }

    @Test
    @DisplayName("applyCoupon: Percentage discount with maxDiscountAmount cap")
    void testApplyCoupon_Percentage_WithCap() {
        activeCoupon.setDiscountType(DiscountType.PERCENTAGE);
        activeCoupon.setDiscountValue(10.0); // 10%
        activeCoupon.setMaxDiscountAmount(300.0); // Cap at 300
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        // 10% of 5000 is 500, but cap is 300
        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(5000.0)
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertTrue(res.getIsValid());
        assertEquals(300.0, res.getDiscountAmount());
        assertEquals(4700.0, res.getFinalTotal());
    }

    @Test
    @DisplayName("applyCoupon: Flat discount capped at cartTotal if discount > total")
    void testApplyCoupon_FlatCappedAtCartTotal() {
        activeCoupon.setDiscountValue(800.0);
        activeCoupon.setMinOrderValue(100.0);
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(500.0)
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertTrue(res.getIsValid());
        assertEquals(500.0, res.getDiscountAmount());
        assertEquals(0.0, res.getFinalTotal());
    }

    @Test
    @DisplayName("applyCoupon: CATEGORIES targeting passes when cart has item in target categories")
    void testApplyCoupon_Categories_Valid() {
        activeCoupon.setTargetType(CouponTargetType.CATEGORIES);
        activeCoupon.setTargetCategoryIds(List.of("cat-car-battery", "cat-bike-battery"));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(2500.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-1")
                                .categoryId("cat-car-battery")
                                .productPrice(2500.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertTrue(res.getIsValid());
        assertEquals(100.0, res.getDiscountAmount());
    }

    @Test
    @DisplayName("applyCoupon: CATEGORIES targeting fails when cart has no items in target categories")
    void testApplyCoupon_Categories_Invalid() {
        activeCoupon.setTargetType(CouponTargetType.CATEGORIES);
        activeCoupon.setTargetCategoryIds(List.of("cat-car-battery"));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(2500.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-1")
                                .categoryId("cat-solar-inverter")
                                .productPrice(2500.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertFalse(res.getIsValid());
        assertEquals("Coupon is only valid for eligible product categories", res.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: BRANDS targeting passes when cart has item in target brands")
    void testApplyCoupon_Brands_Valid() {
        activeCoupon.setTargetType(CouponTargetType.BRANDS);
        activeCoupon.setTargetBrandIds(List.of("brand-exide", "brand-amaron"));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(3500.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-1")
                                .brandId("brand-exide")
                                .productPrice(3500.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertTrue(res.getIsValid());
        assertEquals(100.0, res.getDiscountAmount());
    }

    @Test
    @DisplayName("applyCoupon: BRANDS targeting fails when cart has no item from target brands")
    void testApplyCoupon_Brands_Invalid() {
        activeCoupon.setTargetType(CouponTargetType.BRANDS);
        activeCoupon.setTargetBrandIds(List.of("brand-exide"));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(3500.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-1")
                                .brandId("brand-luminous")
                                .productPrice(3500.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertFalse(res.getIsValid());
        assertEquals("Coupon is only valid for eligible brands", res.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: CUSTOM_PRODUCTS targeting passes when cart contains targeted product")
    void testApplyCoupon_CustomProducts_Valid() {
        activeCoupon.setTargetType(CouponTargetType.CUSTOM_PRODUCTS);
        activeCoupon.setTargetProductIds(List.of("prod-target-1", "prod-target-2"));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(4000.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-target-1")
                                .productPrice(4000.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertTrue(res.getIsValid());
        assertEquals(100.0, res.getDiscountAmount());
    }

    @Test
    @DisplayName("applyCoupon: CUSTOM_PRODUCTS targeting fails when cart does not contain targeted product")
    void testApplyCoupon_CustomProducts_Invalid() {
        activeCoupon.setTargetType(CouponTargetType.CUSTOM_PRODUCTS);
        activeCoupon.setTargetProductIds(List.of("prod-target-1"));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(4000.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-other")
                                .productPrice(4000.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertFalse(res.getIsValid());
        assertEquals("Coupon is only valid for eligible products", res.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: EXCLUDED_PRODUCTS fails if cart contains any excluded product")
    void testApplyCoupon_ExcludedProducts_ContainsExcluded() {
        activeCoupon.setTargetType(CouponTargetType.EXCLUDED_PRODUCTS);
        activeCoupon.setExcludedProductIds(List.of("prod-excluded-1"));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(5000.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-excluded-1")
                                .productPrice(2500.0)
                                .quantity(1)
                                .build(),
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-clean-2")
                                .productPrice(2500.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertFalse(res.getIsValid());
        assertEquals("Coupon cannot be applied because cart contains excluded products", res.getMessage());
    }

    @Test
    @DisplayName("applyCoupon: EXCLUDED_PRODUCTS succeeds if cart contains no excluded product")
    void testApplyCoupon_ExcludedProducts_CleanCart() {
        activeCoupon.setTargetType(CouponTargetType.EXCLUDED_PRODUCTS);
        activeCoupon.setExcludedProductIds(List.of("prod-excluded-1"));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(5000.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId("prod-clean-2")
                                .productPrice(5000.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertTrue(res.getIsValid());
        assertEquals(100.0, res.getDiscountAmount());
    }

    @Test
    @DisplayName("applyCoupon: Enriches missing category and brand from ProductRepository")
    void testApplyCoupon_EnrichmentFromProductRepository() {
        UUID prodUuid = UUID.randomUUID();
        UUID catUuid = UUID.randomUUID();
        UUID brandUuid = UUID.randomUUID();

        Product product = new Product();
        product.setProductId(prodUuid);

        Category cat = new Category();
        cat.setCategoryId(catUuid);
        product.setProductCategory(cat);

        Brand brand = new Brand();
        brand.setBrandId(brandUuid);
        product.setBrand(brand);

        when(productRepository.findById(prodUuid)).thenReturn(Optional.of(product));

        activeCoupon.setTargetType(CouponTargetType.BRANDS);
        activeCoupon.setTargetBrandIds(List.of(brandUuid.toString()));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .code("SAVE100")
                .cartTotal(3000.0)
                .cartItems(List.of(
                        ApplyCouponRequest.ApplyCouponCartItemDto.builder()
                                .productId(prodUuid.toString())
                                // brandId and categoryId intentionally omitted
                                .productPrice(3000.0)
                                .quantity(1)
                                .build()
                ))
                .build();

        ApplyCouponResponse res = couponService.applyCoupon(request);
        assertTrue(res.getIsValid());
        assertEquals(100.0, res.getDiscountAmount());
    }

    @Test
    @DisplayName("applyCoupon: Backward-compatible applyCoupon(code, cartTotal) overload works")
    void testApplyCoupon_BackwardCompatibleOverload() {
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));

        ApplyCouponResponse res = couponService.applyCoupon("SAVE100", 1200.0);
        assertTrue(res.getIsValid());
        assertEquals(100.0, res.getDiscountAmount());
        assertEquals(1100.0, res.getFinalTotal());
    }

    @Test
    @DisplayName("Admin CRUD: createCoupon persists targeting fields and defaults")
    void testCreateCoupon() {
        CouponRequest req = CouponRequest.builder()
                .code("EXIDE200")
                .discountType(DiscountType.FLAT)
                .discountValue(200.0)
                .targetType(CouponTargetType.BRANDS)
                .targetBrandIds(List.of("brand-exide"))
                .targetBrandNames(List.of("Exide"))
                .displayOnPdp(true)
                .pdpHighlightText("Get ₹200 off Exide batteries")
                .build();

        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CouponResponse res = couponService.createCoupon(req);
        assertNotNull(res);
        assertEquals("EXIDE200", res.getCode());
        assertEquals(CouponTargetType.BRANDS, res.getTargetType());
        assertEquals(List.of("brand-exide"), res.getTargetBrandIds());
        assertEquals(List.of("Exide"), res.getTargetBrandNames());
        assertTrue(res.getDisplayOnPdp());
        assertEquals("Get ₹200 off Exide batteries", res.getPdpHighlightText());
    }

    @Test
    @DisplayName("Admin CRUD: updateCoupon updates targeting fields")
    void testUpdateCoupon() {
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(activeCoupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CouponRequest req = CouponRequest.builder()
                .code("SAVE100")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(15.0)
                .targetType(CouponTargetType.CATEGORIES)
                .targetCategoryIds(List.of("cat-bike"))
                .displayOnPdp(false)
                .build();

        CouponResponse res = couponService.updateCoupon(couponId, req);
        assertEquals(DiscountType.PERCENTAGE, res.getDiscountType());
        assertEquals(15.0, res.getDiscountValue());
        assertEquals(CouponTargetType.CATEGORIES, res.getTargetType());
        assertEquals(List.of("cat-bike"), res.getTargetCategoryIds());
        assertFalse(res.getDisplayOnPdp());
    }

    @Test
    @DisplayName("Admin CRUD: incrementCouponUsage increments usedCount")
    void testIncrementCouponUsage() {
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(activeCoupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        couponService.incrementCouponUsage("SAVE100");
        assertEquals(6, activeCoupon.getUsedCount());
        verify(couponRepository, times(1)).save(activeCoupon);
    }
}

