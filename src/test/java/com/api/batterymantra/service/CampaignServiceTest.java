package com.api.batterymantra.service;

import com.api.batterymantra.dto.campaign.*;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.*;
import com.api.batterymantra.repository.BrandRepository;
import com.api.batterymantra.repository.CampaignRepository;
import com.api.batterymantra.repository.CategoryRepository;
import com.api.batterymantra.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CampaignService campaignService;

    private Campaign diwaliCampaign;
    private Product testProduct;
    private Category testCategory;
    private Brand testBrand;
    private UUID productId;
    private UUID categoryId;
    private UUID brandId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        brandId = UUID.randomUUID();

        testCategory = new Category();
        testCategory.setCategoryId(categoryId);
        testCategory.setCategoryName("Car Batteries");

        testBrand = new Brand();
        testBrand.setBrandId(brandId);
        testBrand.setBrandName("Exide");

        testProduct = new Product();
        testProduct.setProductId(productId);
        testProduct.setProductName("Exide Matrix 35Ah");
        testProduct.setProductPrice(BigDecimal.valueOf(4500));
        testProduct.setExchangeDiscount(BigDecimal.valueOf(500));
        testProduct.setProductCategory(testCategory);
        testProduct.setCategories(Collections.singleton(testCategory));
        testProduct.setBrand(testBrand);
        testProduct.setApproved(true);

        diwaliCampaign = Campaign.builder()
                .id(UUID.randomUUID())
                .name("Diwali Dhamaka")
                .slug("diwali-dhamaka")
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .discountValue(BigDecimal.valueOf(500))
                .minOrderValue(BigDecimal.valueOf(2000))
                .startDate(OffsetDateTime.now().minusDays(2))
                .endDate(OffsetDateTime.now().plusDays(5))
                .isActive(true)
                .priority(10)
                .couponCode("DIWALI500")
                .build();
    }

    // ==========================================
    // 1. Discount Math Tests
    // ==========================================

    @Test
    @DisplayName("Discount: Flat amount discount calculation")
    void testCalculateDiscount_FlatAmount() {
        Campaign campaign = Campaign.builder()
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .discountValue(BigDecimal.valueOf(500))
                .build();

        BigDecimal discount = campaignService.calculateDiscount(campaign, BigDecimal.valueOf(4500), BigDecimal.valueOf(4500));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(discount));
    }

    @Test
    @DisplayName("Discount: Percentage discount without cap")
    void testCalculateDiscount_PercentageWithoutCap() {
        Campaign campaign = Campaign.builder()
                .discountType(CampaignDiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10)) // 10%
                .build();

        BigDecimal discount = campaignService.calculateDiscount(campaign, BigDecimal.valueOf(4000), BigDecimal.valueOf(4000));
        assertEquals(0, BigDecimal.valueOf(400.00).compareTo(discount));
    }

    @Test
    @DisplayName("Discount: Percentage discount capped at maxDiscountCap")
    void testCalculateDiscount_PercentageWithCap() {
        Campaign campaign = Campaign.builder()
                .discountType(CampaignDiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10)) // 10% of 10,000 = 1,000
                .maxDiscountCap(BigDecimal.valueOf(600)) // Cap is 600
                .build();

        BigDecimal discount = campaignService.calculateDiscount(campaign, BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));
        assertEquals(0, BigDecimal.valueOf(600).compareTo(discount));
    }

    @Test
    @DisplayName("Discount: Extra exchange bonus adds bonus value")
    void testCalculateDiscount_ExtraExchangeBonus() {
        Campaign campaign = Campaign.builder()
                .discountType(CampaignDiscountType.EXTRA_EXCHANGE_BONUS)
                .discountValue(BigDecimal.valueOf(300))
                .build();

        BigDecimal discount = campaignService.calculateDiscount(campaign, BigDecimal.valueOf(4000), BigDecimal.valueOf(3500));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(discount));
    }

    @Test
    @DisplayName("Discount: Showcase only returns zero discount")
    void testCalculateDiscount_ShowcaseOnly() {
        Campaign campaign = Campaign.builder()
                .discountType(CampaignDiscountType.SHOWCASE_ONLY)
                .discountValue(BigDecimal.valueOf(500))
                .build();

        BigDecimal discount = campaignService.calculateDiscount(campaign, BigDecimal.valueOf(4000), BigDecimal.valueOf(4000));
        assertEquals(0, BigDecimal.ZERO.compareTo(discount));
    }

    @Test
    @DisplayName("Discount: Discount cannot exceed base payable amount")
    void testCalculateDiscount_CappedAtBasePayable() {
        Campaign campaign = Campaign.builder()
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .discountValue(BigDecimal.valueOf(1000))
                .build();

        BigDecimal discount = campaignService.calculateDiscount(campaign, BigDecimal.valueOf(600), BigDecimal.valueOf(400));
        assertEquals(0, BigDecimal.valueOf(400).compareTo(discount));
    }

    // ==========================================
    // 2. Product Eligibility Tests
    // ==========================================

    @Test
    @DisplayName("Eligibility: ALL_PRODUCTS matches any product except excluded")
    void testIsProductEligible_AllProducts() {
        Campaign campaign = Campaign.builder()
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .build();

        assertTrue(campaignService.isProductEligible(campaign, testProduct));

        // Add to excluded
        campaign.setExcludedProductIds(Collections.singleton(productId));
        assertFalse(campaignService.isProductEligible(campaign, testProduct));
    }

    @Test
    @DisplayName("Eligibility: CATEGORIES matches product category")
    void testIsProductEligible_Categories() {
        Campaign campaign = Campaign.builder()
                .targetType(CampaignTargetType.CATEGORIES)
                .categoryIds(Collections.singleton(categoryId))
                .build();

        assertTrue(campaignService.isProductEligible(campaign, testProduct));

        // Non-matching category
        campaign.setCategoryIds(Collections.singleton(UUID.randomUUID()));
        assertFalse(campaignService.isProductEligible(campaign, testProduct));
    }

    @Test
    @DisplayName("Eligibility: BRANDS matches product brand")
    void testIsProductEligible_Brands() {
        Campaign campaign = Campaign.builder()
                .targetType(CampaignTargetType.BRANDS)
                .brandIds(Collections.singleton(brandId))
                .build();

        assertTrue(campaignService.isProductEligible(campaign, testProduct));

        // Non-matching brand
        campaign.setBrandIds(Collections.singleton(UUID.randomUUID()));
        assertFalse(campaignService.isProductEligible(campaign, testProduct));
    }

    @Test
    @DisplayName("Eligibility: CUSTOM_PRODUCTS matches specific product IDs")
    void testIsProductEligible_CustomProducts() {
        Campaign campaign = Campaign.builder()
                .targetType(CampaignTargetType.CUSTOM_PRODUCTS)
                .productIds(Collections.singleton(productId))
                .build();

        assertTrue(campaignService.isProductEligible(campaign, testProduct));

        // Non-matching product
        campaign.setProductIds(Collections.singleton(UUID.randomUUID()));
        assertFalse(campaignService.isProductEligible(campaign, testProduct));
    }

    // ==========================================
    // 3. Cart Validation & Evaluation Tests
    // ==========================================

    @Test
    @DisplayName("ValidateCart: Auto-apply chooses highest priority campaign")
    void testValidateCart_AutoApply_HighestPriority() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        Campaign lowPriority = Campaign.builder()
                .id(UUID.randomUUID())
                .name("Monsoon Saver")
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .discountValue(BigDecimal.valueOf(200))
                .priority(1)
                .isActive(true)
                .startDate(OffsetDateTime.now().minusDays(1))
                .endDate(OffsetDateTime.now().plusDays(2))
                .build();

        Campaign highPriority = Campaign.builder()
                .id(UUID.randomUUID())
                .name("Mega Diwali Dhamaka")
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .discountValue(BigDecimal.valueOf(500))
                .priority(10)
                .isActive(true)
                .startDate(OffsetDateTime.now().minusDays(1))
                .endDate(OffsetDateTime.now().plusDays(2))
                .build();

        // Repository returns sorted by priority DESC
        when(campaignRepository.findActiveAutoApplyCampaigns(any())).thenReturn(List.of(highPriority, lowPriority));

        ValidateCartRequest req = ValidateCartRequest.builder()
                .items(List.of(CartValidateItemDto.builder()
                        .productId(productId)
                        .quantity(1)
                        .hasExchange(false)
                        .build()))
                .build();

        ValidateCartResponse res = campaignService.validateCart(req);

        assertTrue(res.isApplicable());
        assertEquals(highPriority.getId(), res.getCampaignId());
        assertEquals(highPriority.getName(), res.getCampaignName());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(res.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(4000).compareTo(res.getFinalPayable())); // 4500 - 500 = 4000
    }

    @Test
    @DisplayName("ValidateCart: Valid coupon code applies offer")
    void testValidateCart_CouponCode_Success() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(campaignRepository.findByCouponCodeIgnoreCase("DIWALI500")).thenReturn(Optional.of(diwaliCampaign));

        ValidateCartRequest req = ValidateCartRequest.builder()
                .couponCode("DIWALI500")
                .items(List.of(CartValidateItemDto.builder()
                        .productId(productId)
                        .quantity(1)
                        .hasExchange(true)
                        .build()))
                .build();

        ValidateCartResponse res = campaignService.validateCart(req);

        assertTrue(res.isApplicable());
        assertEquals(diwaliCampaign.getId(), res.getCampaignId());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(res.getDiscountAmount()));
        // Base payable: 4500 - 500 (normal exchange) = 4000. Final payable: 4000 - 500 (campaign) = 3500
        assertEquals(0, BigDecimal.valueOf(3500).compareTo(res.getFinalPayable()));
    }

    @Test
    @DisplayName("ValidateCart: Invalid coupon code returns not applicable")
    void testValidateCart_CouponCode_Invalid() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(campaignRepository.findByCouponCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        ValidateCartRequest req = ValidateCartRequest.builder()
                .couponCode("UNKNOWN")
                .items(List.of(CartValidateItemDto.builder()
                        .productId(productId)
                        .quantity(1)
                        .hasExchange(false)
                        .build()))
                .build();

        ValidateCartResponse res = campaignService.validateCart(req);

        assertFalse(res.isApplicable());
        assertEquals("Invalid coupon code", res.getMessage());
    }

    @Test
    @DisplayName("ValidateCart: Min order value not met returns not applicable")
    void testValidateCart_MinOrderValueNotMet() {
        diwaliCampaign.setMinOrderValue(BigDecimal.valueOf(10000)); // higher than product price 4500

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(campaignRepository.findByCouponCodeIgnoreCase("DIWALI500")).thenReturn(Optional.of(diwaliCampaign));

        ValidateCartRequest req = ValidateCartRequest.builder()
                .couponCode("DIWALI500")
                .items(List.of(CartValidateItemDto.builder()
                        .productId(productId)
                        .quantity(1)
                        .hasExchange(false)
                        .build()))
                .build();

        ValidateCartResponse res = campaignService.validateCart(req);

        assertFalse(res.isApplicable());
        assertTrue(res.getMessage().contains("minimum order value"));
    }

    @Test
    @DisplayName("ValidateCart: EXTRA_EXCHANGE_BONUS requires exchange trade-in")
    void testValidateCart_ExtraExchangeBonus_RequiresExchange() {
        diwaliCampaign.setDiscountType(CampaignDiscountType.EXTRA_EXCHANGE_BONUS);

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(campaignRepository.findByCouponCodeIgnoreCase("DIWALI500")).thenReturn(Optional.of(diwaliCampaign));

        ValidateCartRequest req = ValidateCartRequest.builder()
                .couponCode("DIWALI500")
                .items(List.of(CartValidateItemDto.builder()
                        .productId(productId)
                        .quantity(1)
                        .hasExchange(false) // No trade-in selected
                        .build()))
                .build();

        ValidateCartResponse res = campaignService.validateCart(req);

        assertFalse(res.isApplicable());
        assertTrue(res.getMessage().contains("trade-in is required"));
    }

    // ==========================================
    // 4. Admin CRUD & Lifecycle Tests
    // ==========================================

    @Test
    @DisplayName("Create Campaign: End date before start date throws exception")
    void testCreateCampaign_InvalidDates() {
        CampaignRequest req = CampaignRequest.builder()
                .name("Invalid Dates Campaign")
                .startDate(OffsetDateTime.now().plusDays(5))
                .endDate(OffsetDateTime.now().plusDays(2)) // Before start date
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .build();

        assertThrows(ResponseStatusException.class, () -> campaignService.createCampaign(req));
    }

    @Test
    @DisplayName("Create Campaign: Generates valid slug from name if slug is empty")
    void testCreateCampaign_GeneratesSlug() {
        CampaignRequest req = CampaignRequest.builder()
                .name("Winter Saver 2026!")
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(10))
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .build();

        when(campaignRepository.existsBySlug("winter-saver-2026")).thenReturn(false);
        when(campaignRepository.save(any())).thenAnswer(invocation -> {
            Campaign c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CampaignResponse res = campaignService.createCampaign(req);

        assertNotNull(res);
        assertEquals("winter-saver-2026", res.getSlug());
    }

    @Test
    @DisplayName("Toggle Campaign Status: Inverts isActive flag")
    void testToggleCampaignStatus() {
        UUID id = UUID.randomUUID();
        Campaign campaign = Campaign.builder()
                .id(id)
                .name("Flash Sale")
                .slug("flash-sale")
                .isActive(true)
                .targetType(CampaignTargetType.ALL_PRODUCTS)
                .discountType(CampaignDiscountType.FLAT_AMOUNT)
                .startDate(OffsetDateTime.now().minusDays(1))
                .endDate(OffsetDateTime.now().plusDays(1))
                .build();

        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignResponse res = campaignService.toggleCampaignStatus(id);

        assertFalse(res.getIsActive());
        assertEquals("INACTIVE", res.getStatus());
    }
}
