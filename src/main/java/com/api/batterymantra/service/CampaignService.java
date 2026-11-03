package com.api.batterymantra.service;

import com.api.batterymantra.dto.campaign.*;
import com.api.batterymantra.dto.product.ProductListResponse;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.CampaignDiscountType;
import com.api.batterymantra.entity.enums.CampaignStatusFilter;
import com.api.batterymantra.entity.enums.CampaignTargetType;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.BrandRepository;
import com.api.batterymantra.repository.CampaignRepository;
import com.api.batterymantra.repository.CategoryRepository;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.specification.ProductSpecification;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductService productService;

    // ==========================================
    // Public / Storefront Methods
    // ==========================================

    @Transactional(readOnly = true)
    public List<CampaignResponse> getActiveCampaigns() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Campaign> activeCampaigns = campaignRepository.findActiveCampaigns(now);
        return activeCampaigns.stream()
                .map(c -> toCampaignResponse(c, now))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CampaignLandingResponse getCampaignBySlug(
            String slug,
            int page,
            int size,
            String category,
            String brand,
            String sort,
            UUID cityId) {

        Campaign campaign = campaignRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with slug: " + slug));

        Specification<Product> spec = buildParticipatingProductsSpecification(campaign, category, brand);
        Sort sortObj = resolveProductSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<ProductListResponse> productPage = productRepository.findAll(spec, pageable)
                .map(p -> productService.toListResponse(p, cityId));

        OffsetDateTime now = OffsetDateTime.now();
        return CampaignLandingResponse.builder()
                .campaign(toCampaignResponse(campaign, now))
                .products(productPage)
                .build();
    }

    @Transactional(readOnly = true)
    public ValidateCartResponse validateCart(ValidateCartRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ValidateCartResponse.builder()
                    .applicable(false)
                    .discountAmount(BigDecimal.ZERO)
                    .finalPayable(BigDecimal.ZERO)
                    .message("Cart is empty")
                    .build();
        }

        // 1. Calculate base cart figures
        BigDecimal grossSubtotal = BigDecimal.ZERO;
        BigDecimal totalNormalExchangeDiscount = BigDecimal.ZERO;
        List<CartProductInfo> cartProducts = new ArrayList<>();

        for (CartValidateItemDto item : request.getItems()) {
            if (item == null || item.getProductId() == null) continue;

            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null) continue;

            int qty = Math.max(1, item.getQuantity());
            BigDecimal unitPrice = product.getProductPrice() != null ? product.getProductPrice() : BigDecimal.ZERO;
            BigDecimal exchangeDiscount = product.getExchangeDiscount() != null ? product.getExchangeDiscount() : BigDecimal.ZERO;

            // City pricing override if provided
            if (request.getCityId() != null && product.getCityPrices() != null) {
                for (ProductCityPricing cp : product.getCityPrices()) {
                    if (cp.getCity() != null && request.getCityId().equals(cp.getCity().getCityId())) {
                        if (cp.getPrice() != null && cp.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                            unitPrice = cp.getPrice();
                        }
                        if (cp.getExchangeDiscount() != null) {
                            exchangeDiscount = cp.getExchangeDiscount();
                        }
                        break;
                    }
                }
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            grossSubtotal = grossSubtotal.add(lineTotal);

            BigDecimal lineExchangeDisc = BigDecimal.ZERO;
            if (item.isHasExchange()) {
                lineExchangeDisc = exchangeDiscount.multiply(BigDecimal.valueOf(qty));
                totalNormalExchangeDiscount = totalNormalExchangeDiscount.add(lineExchangeDisc);
            }

            cartProducts.add(new CartProductInfo(product, qty, item.isHasExchange(), lineTotal, lineExchangeDisc));
        }

        BigDecimal basePayable = grossSubtotal.subtract(totalNormalExchangeDiscount);
        if (basePayable.compareTo(BigDecimal.ZERO) < 0) {
            basePayable = BigDecimal.ZERO;
        }

        OffsetDateTime now = OffsetDateTime.now();
        String couponCode = request.getCouponCode() != null ? request.getCouponCode().trim() : "";

        // 2. Coupon vs Auto-Apply evaluation
        if (!couponCode.isEmpty()) {
            // User sent a specific coupon code
            Optional<Campaign> campaignOpt = campaignRepository.findByCouponCodeIgnoreCase(couponCode);
            if (campaignOpt.isEmpty()) {
                return ValidateCartResponse.builder()
                        .applicable(false)
                        .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .finalPayable(basePayable.setScale(2, RoundingMode.HALF_UP))
                        .message("Invalid coupon code")
                        .build();
            }

            Campaign campaign = campaignOpt.get();
            if (!isCampaignCurrentlyActive(campaign, now)) {
                return ValidateCartResponse.builder()
                        .applicable(false)
                        .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .finalPayable(basePayable.setScale(2, RoundingMode.HALF_UP))
                        .message("Campaign offer is not currently active")
                        .build();
            }

            List<CartProductInfo> eligibleItems = getEligibleCartItems(campaign, cartProducts);
            if (eligibleItems.isEmpty()) {
                return ValidateCartResponse.builder()
                        .applicable(false)
                        .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .finalPayable(basePayable.setScale(2, RoundingMode.HALF_UP))
                        .message("No items in your cart qualify for this campaign offer")
                        .build();
            }

            if (campaign.getMinOrderValue() != null && grossSubtotal.compareTo(campaign.getMinOrderValue()) < 0) {
                return ValidateCartResponse.builder()
                        .applicable(false)
                        .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .finalPayable(basePayable.setScale(2, RoundingMode.HALF_UP))
                        .message("Cart minimum order value of ₹" + campaign.getMinOrderValue() + " not met")
                        .build();
            }

            if (campaign.getDiscountType() == CampaignDiscountType.EXTRA_EXCHANGE_BONUS) {
                boolean hasExchangeEligible = eligibleItems.stream().anyMatch(CartProductInfo::hasExchange);
                if (!hasExchangeEligible) {
                    return ValidateCartResponse.builder()
                            .applicable(false)
                            .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .finalPayable(basePayable.setScale(2, RoundingMode.HALF_UP))
                            .message("Old battery exchange trade-in is required to unlock this offer")
                            .build();
                }
            }

            BigDecimal discountAmount = calculateCartDiscount(campaign, eligibleItems, grossSubtotal, basePayable);
            BigDecimal finalPayable = basePayable.subtract(discountAmount);
            if (finalPayable.compareTo(BigDecimal.ZERO) < 0) {
                finalPayable = BigDecimal.ZERO;
            }

            return ValidateCartResponse.builder()
                    .applicable(true)
                    .campaignId(campaign.getId())
                    .campaignName(campaign.getName())
                    .discountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP))
                    .finalPayable(finalPayable.setScale(2, RoundingMode.HALF_UP))
                    .message("Offer applied successfully!")
                    .build();

        } else {
            // Auto-apply logic: prioritize by priority DESC, discount_value DESC
            List<Campaign> autoCampaigns = campaignRepository.findActiveAutoApplyCampaigns(now);

            for (Campaign campaign : autoCampaigns) {
                List<CartProductInfo> eligibleItems = getEligibleCartItems(campaign, cartProducts);
                if (eligibleItems.isEmpty()) {
                    continue;
                }

                if (campaign.getMinOrderValue() != null && grossSubtotal.compareTo(campaign.getMinOrderValue()) < 0) {
                    continue;
                }

                if (campaign.getDiscountType() == CampaignDiscountType.EXTRA_EXCHANGE_BONUS) {
                    boolean hasExchangeEligible = eligibleItems.stream().anyMatch(CartProductInfo::hasExchange);
                    if (!hasExchangeEligible) {
                        continue;
                    }
                }

                BigDecimal discountAmount = calculateCartDiscount(campaign, eligibleItems, grossSubtotal, basePayable);
                BigDecimal finalPayable = basePayable.subtract(discountAmount);
                if (finalPayable.compareTo(BigDecimal.ZERO) < 0) {
                    finalPayable = BigDecimal.ZERO;
                }

                return ValidateCartResponse.builder()
                        .applicable(true)
                        .campaignId(campaign.getId())
                        .campaignName(campaign.getName())
                        .discountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP))
                        .finalPayable(finalPayable.setScale(2, RoundingMode.HALF_UP))
                        .message("Offer applied successfully!")
                        .build();
            }

            // No active auto-apply campaign matched
            return ValidateCartResponse.builder()
                    .applicable(false)
                    .campaignId(null)
                    .campaignName(null)
                    .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .finalPayable(basePayable.setScale(2, RoundingMode.HALF_UP))
                    .message("No offer applicable")
                    .build();
        }
    }

    // ==========================================
    // Admin Management Methods
    // ==========================================

    @Transactional(readOnly = true)
    public Page<CampaignResponse> getAllCampaigns(CampaignStatusFilter status, String search, Pageable pageable) {
        OffsetDateTime now = OffsetDateTime.now();

        Specification<Campaign> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && status != CampaignStatusFilter.ALL) {
                switch (status) {
                    case ACTIVE:
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), now));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), now));
                        break;
                    case SCHEDULED:
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.greaterThan(root.get("startDate"), now));
                        break;
                    case EXPIRED:
                        predicates.add(cb.lessThan(root.get("endDate"), now));
                        break;
                }
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate slugMatch = cb.like(cb.lower(root.get("slug")), searchPattern);
                Predicate couponMatch = cb.like(cb.lower(root.get("couponCode")), searchPattern);
                predicates.add(cb.or(nameMatch, slugMatch, couponMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return campaignRepository.findAll(spec, pageable).map(c -> toCampaignResponse(c, now));
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        return toCampaignResponse(campaign, OffsetDateTime.now());
    }

    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        validateCampaignDates(request.getStartDate(), request.getEndDate());
        validateProductDiscounts(request);

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? slugify(request.getSlug())
                : slugify(request.getName());

        if (campaignRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign with slug '" + slug + "' already exists");
        }

        Campaign campaign = new Campaign();
        mapRequestToEntity(request, campaign);
        campaign.setSlug(slug);

        Campaign saved = campaignRepository.save(campaign);
        return toCampaignResponse(saved, OffsetDateTime.now());
    }

    @Transactional
    public CampaignResponse updateCampaign(UUID id, CampaignRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        validateCampaignDates(request.getStartDate(), request.getEndDate());
        validateProductDiscounts(request);

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? slugify(request.getSlug())
                : campaign.getSlug();

        if (campaignRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Another campaign with slug '" + slug + "' already exists");
        }

        mapRequestToEntity(request, campaign);
        campaign.setSlug(slug);

        Campaign updated = campaignRepository.save(campaign);
        return toCampaignResponse(updated, OffsetDateTime.now());
    }

    @Transactional
    public void deleteCampaign(UUID id) {
        if (!campaignRepository.existsById(id)) {
            throw new ResourceNotFoundException("Campaign not found with id: " + id);
        }
        campaignRepository.deleteById(id);
    }

    @Transactional
    public CampaignResponse toggleCampaignStatus(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        campaign.setIsActive(!Boolean.TRUE.equals(campaign.getIsActive()));
        Campaign saved = campaignRepository.save(campaign);
        return toCampaignResponse(saved, OffsetDateTime.now());
    }

    // ==========================================
    // Business Logic Helpers
    // ==========================================

    public boolean isProductEligible(Campaign campaign, Product product) {
        if (campaign == null || product == null) {
            return false;
        }

        // Check exclusions first
        if (campaign.getExcludedProductIds() != null &&
                campaign.getExcludedProductIds().contains(product.getProductId())) {
            return false;
        }

        switch (campaign.getTargetType()) {
            case ALL_PRODUCTS:
                return true;

            case CATEGORIES:
                if (campaign.getCategoryIds() == null || campaign.getCategoryIds().isEmpty()) {
                    return false;
                }
                // Check primary category
                if (product.getProductCategory() != null &&
                        campaign.getCategoryIds().contains(product.getProductCategory().getCategoryId())) {
                    return true;
                }
                // Check secondary categories
                if (product.getCategories() != null) {
                    return product.getCategories().stream()
                            .anyMatch(c -> c != null && campaign.getCategoryIds().contains(c.getCategoryId()));
                }
                return false;

            case BRANDS:
                if (campaign.getBrandIds() == null || campaign.getBrandIds().isEmpty()) {
                    return false;
                }
                return product.getBrand() != null &&
                        campaign.getBrandIds().contains(product.getBrand().getBrandId());

            case CUSTOM_PRODUCTS:
                if (campaign.getProductIds() == null || campaign.getProductIds().isEmpty()) {
                    return false;
                }
                return campaign.getProductIds().contains(product.getProductId());

            default:
                return false;
        }
    }

    public BigDecimal calculateDiscount(Campaign campaign, BigDecimal subtotal, BigDecimal basePayable) {
        if (campaign == null || campaign.getDiscountType() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal val = campaign.getDiscountValue() != null ? campaign.getDiscountValue() : BigDecimal.ZERO;

        switch (campaign.getDiscountType()) {
            case FLAT_AMOUNT:
                discount = val;
                break;

            case PERCENTAGE:
                discount = subtotal.multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (campaign.getMaxDiscountCap() != null && campaign.getMaxDiscountCap().compareTo(BigDecimal.ZERO) > 0) {
                    discount = discount.min(campaign.getMaxDiscountCap());
                }
                break;

            case EXTRA_EXCHANGE_BONUS:
                discount = val;
                break;

            case SHOWCASE_ONLY:
                discount = BigDecimal.ZERO;
                break;
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        if (basePayable != null && discount.compareTo(basePayable) > 0) {
            discount = basePayable;
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCartDiscount(
            Campaign campaign,
            List<CartProductInfo> eligibleItems,
            BigDecimal grossSubtotal,
            BigDecimal basePayable) {

        if (campaign == null) {
            return BigDecimal.ZERO;
        }

        Map<UUID, BigDecimal> customDiscounts = campaign.getProductDiscounts();
        if (customDiscounts == null || customDiscounts.isEmpty()) {
            return calculateDiscount(campaign, grossSubtotal, basePayable);
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (CartProductInfo item : eligibleItems) {
            if (item == null || item.product() == null) continue;
            UUID pId = item.product().getProductId();

            if (pId != null && customDiscounts.containsKey(pId)) {
                BigDecimal customDisc = customDiscounts.get(pId);
                if (customDisc != null && customDisc.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal lineDisc = customDisc.multiply(BigDecimal.valueOf(item.quantity()));
                    totalDiscount = totalDiscount.add(lineDisc);
                }
            } else {
                BigDecimal defaultItemDiscount = calculateItemDefaultDiscount(campaign, item);
                totalDiscount = totalDiscount.add(defaultItemDiscount);
            }
        }

        if (totalDiscount.compareTo(BigDecimal.ZERO) < 0) {
            totalDiscount = BigDecimal.ZERO;
        }
        if (basePayable != null && totalDiscount.compareTo(basePayable) > 0) {
            totalDiscount = basePayable;
        }

        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateItemDefaultDiscount(Campaign campaign, CartProductInfo item) {
        if (campaign == null || campaign.getDiscountType() == null || item == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal val = campaign.getDiscountValue() != null ? campaign.getDiscountValue() : BigDecimal.ZERO;

        switch (campaign.getDiscountType()) {
            case FLAT_AMOUNT:
                discount = val.multiply(BigDecimal.valueOf(item.quantity()));
                break;

            case PERCENTAGE:
                BigDecimal lineSubtotal = item.lineTotal() != null ? item.lineTotal() : BigDecimal.ZERO;
                discount = lineSubtotal.multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (campaign.getMaxDiscountCap() != null && campaign.getMaxDiscountCap().compareTo(BigDecimal.ZERO) > 0) {
                    discount = discount.min(campaign.getMaxDiscountCap());
                }
                break;

            case EXTRA_EXCHANGE_BONUS:
                if (item.hasExchange()) {
                    discount = val.multiply(BigDecimal.valueOf(item.quantity()));
                }
                break;

            case SHOWCASE_ONLY:
                discount = BigDecimal.ZERO;
                break;
        }

        return discount;
    }

    private List<CartProductInfo> getEligibleCartItems(Campaign campaign, List<CartProductInfo> items) {
        List<CartProductInfo> eligible = new ArrayList<>();
        for (CartProductInfo item : items) {
            if (isProductEligible(campaign, item.product())) {
                eligible.add(item);
            }
        }
        return eligible;
    }

    private boolean isCampaignCurrentlyActive(Campaign campaign, OffsetDateTime now) {
        if (!Boolean.TRUE.equals(campaign.getIsActive())) {
            return false;
        }
        if (campaign.getStartDate() != null && now.isBefore(campaign.getStartDate())) {
            return false;
        }
        if (campaign.getEndDate() != null && now.isAfter(campaign.getEndDate())) {
            return false;
        }
        return true;
    }

    private Specification<Product> buildParticipatingProductsSpecification(
            Campaign campaign,
            String categoryFilter,
            String brandFilter) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only approved products
            predicates.add(cb.isTrue(root.get("isApproved")));

            // Excluded products exception
            if (campaign.getExcludedProductIds() != null && !campaign.getExcludedProductIds().isEmpty()) {
                predicates.add(cb.not(root.get("productId").in(campaign.getExcludedProductIds())));
            }

            // Target type restriction
            switch (campaign.getTargetType()) {
                case ALL_PRODUCTS:
                    // All products eligible
                    break;

                case CATEGORIES:
                    if (campaign.getCategoryIds() == null || campaign.getCategoryIds().isEmpty()) {
                        predicates.add(cb.disjunction());
                    } else {
                        Specification<Product> catSpec = ProductSpecification.hasCategoryIdIn(new ArrayList<>(campaign.getCategoryIds()));
                        predicates.add(catSpec.toPredicate(root, query, cb));
                    }
                    break;

                case BRANDS:
                    if (campaign.getBrandIds() == null || campaign.getBrandIds().isEmpty()) {
                        predicates.add(cb.disjunction());
                    } else {
                        Specification<Product> brandSpec = ProductSpecification.hasBrandIdIn(new ArrayList<>(campaign.getBrandIds()));
                        predicates.add(brandSpec.toPredicate(root, query, cb));
                    }
                    break;

                case CUSTOM_PRODUCTS:
                    if (campaign.getProductIds() == null || campaign.getProductIds().isEmpty()) {
                        predicates.add(cb.disjunction());
                    } else {
                        predicates.add(root.get("productId").in(campaign.getProductIds()));
                    }
                    break;
            }

            // Optional Category query filter
            if (categoryFilter != null && !categoryFilter.isBlank()) {
                try {
                    UUID catId = UUID.fromString(categoryFilter.trim());
                    Specification<Product> catSpec = ProductSpecification.hasCategoryId(catId);
                    predicates.add(catSpec.toPredicate(root, query, cb));
                } catch (IllegalArgumentException e) {
                    // Try slug or name
                    Predicate primaryNameMatch = cb.equal(cb.lower(root.get("productCategory").get("categoryName")), categoryFilter.trim().toLowerCase());
                    Predicate primarySlugMatch = cb.equal(cb.lower(root.get("productCategory").get("slug")), categoryFilter.trim().toLowerCase());
                    predicates.add(cb.or(primaryNameMatch, primarySlugMatch));
                }
            }

            // Optional Brand query filter
            if (brandFilter != null && !brandFilter.isBlank()) {
                try {
                    UUID bId = UUID.fromString(brandFilter.trim());
                    predicates.add(cb.equal(root.get("brand").get("brandId"), bId));
                } catch (IllegalArgumentException e) {
                    Predicate brandNameMatch = cb.equal(cb.lower(root.get("brand").get("brandName")), brandFilter.trim().toLowerCase());
                    Predicate brandSlugMatch = cb.equal(cb.lower(root.get("brand").get("slug")), brandFilter.trim().toLowerCase());
                    predicates.add(cb.or(brandNameMatch, brandSlugMatch));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort resolveProductSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "globalDisplayOrder")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        switch (sort.toLowerCase()) {
            case "price_asc":
                return Sort.by(Sort.Direction.ASC, "productPrice");
            case "price_desc":
                return Sort.by(Sort.Direction.DESC, "productPrice");
            case "newest":
                return Sort.by(Sort.Direction.DESC, "createdAt");
            case "name_asc":
                return Sort.by(Sort.Direction.ASC, "productName");
            default:
                return Sort.by(Sort.Direction.ASC, "globalDisplayOrder")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }

    private void validateCampaignDates(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and End date are required");
        }
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
    }

    private void validateProductDiscounts(CampaignRequest request) {
        if (request == null) {
            return;
        }
        if (request.getTargetType() == CampaignTargetType.CUSTOM_PRODUCTS) {
            if (request.getProductDiscounts() != null && !request.getProductDiscounts().isEmpty()) {
                Set<UUID> allowedProductIds = request.getProductIds() != null
                        ? request.getProductIds()
                        : Collections.emptySet();
                for (UUID productId : request.getProductDiscounts().keySet()) {
                    if (!allowedProductIds.contains(productId)) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Product discount specified for product ID " + productId + " which is not in campaign productIds"
                        );
                    }
                }
            }
        }
    }

    private void mapRequestToEntity(CampaignRequest req, Campaign c) {
        c.setName(req.getName().trim());
        c.setTagline(req.getTagline());
        c.setDescription(req.getDescription());
        c.setBadgeText(req.getBadgeText());
        c.setBadgeTheme(req.getBadgeTheme() != null ? req.getBadgeTheme() : com.api.batterymantra.entity.enums.BadgeTheme.BRAND_RED);
        c.setDesktopBannerUrl(req.getDesktopBannerUrl());
        c.setMobileBannerUrl(req.getMobileBannerUrl());
        c.setStartDate(req.getStartDate());
        c.setEndDate(req.getEndDate());
        c.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        c.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        c.setShowCountdown(req.getShowCountdown() != null ? req.getShowCountdown() : true);
        c.setShowInAnnouncement(req.getShowInAnnouncement() != null ? req.getShowInAnnouncement() : true);
        c.setShowOnHome(req.getShowOnHome() != null ? req.getShowOnHome() : true);
        c.setTargetType(req.getTargetType());
        c.setDiscountType(req.getDiscountType());
        c.setDiscountValue(req.getDiscountValue() != null ? req.getDiscountValue() : BigDecimal.ZERO);
        c.setMaxDiscountCap(req.getMaxDiscountCap());
        c.setMinOrderValue(req.getMinOrderValue() != null ? req.getMinOrderValue() : BigDecimal.ZERO);

        String trimmedCoupon = (req.getCouponCode() != null && !req.getCouponCode().isBlank())
                ? req.getCouponCode().trim().toUpperCase()
                : null;
        c.setCouponCode(trimmedCoupon);

        c.setCategoryIds(req.getCategoryIds() != null ? new HashSet<>(req.getCategoryIds()) : new HashSet<>());
        c.setBrandIds(req.getBrandIds() != null ? new HashSet<>(req.getBrandIds()) : new HashSet<>());
        c.setProductIds(req.getProductIds() != null ? new HashSet<>(req.getProductIds()) : new HashSet<>());
        c.setExcludedProductIds(req.getExcludedProductIds() != null ? new HashSet<>(req.getExcludedProductIds()) : new HashSet<>());

        if (req.getTargetType() == CampaignTargetType.CUSTOM_PRODUCTS) {
            c.setProductDiscounts(req.getProductDiscounts() != null ? new HashMap<>(req.getProductDiscounts()) : new HashMap<>());
        } else {
            c.setProductDiscounts(new HashMap<>());
        }
    }

    public CampaignResponse toCampaignResponse(Campaign c, OffsetDateTime now) {
        String status = "ACTIVE";
        if (!Boolean.TRUE.equals(c.getIsActive())) {
            status = "INACTIVE";
        } else if (c.getEndDate() != null && now.isAfter(c.getEndDate())) {
            status = "EXPIRED";
        } else if (c.getStartDate() != null && now.isBefore(c.getStartDate())) {
            status = "SCHEDULED";
        }

        return CampaignResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .tagline(c.getTagline())
                .description(c.getDescription())
                .badgeText(c.getBadgeText())
                .badgeTheme(c.getBadgeTheme())
                .desktopBannerUrl(c.getDesktopBannerUrl())
                .mobileBannerUrl(c.getMobileBannerUrl())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .isActive(c.getIsActive())
                .priority(c.getPriority())
                .showCountdown(c.getShowCountdown())
                .showInAnnouncement(c.getShowInAnnouncement())
                .showOnHome(c.getShowOnHome())
                .targetType(c.getTargetType())
                .discountType(c.getDiscountType())
                .discountValue(c.getDiscountValue())
                .maxDiscountCap(c.getMaxDiscountCap())
                .minOrderValue(c.getMinOrderValue())
                .couponCode(c.getCouponCode())
                .categoryIds(c.getCategoryIds() != null ? new HashSet<>(c.getCategoryIds()) : new HashSet<>())
                .brandIds(c.getBrandIds() != null ? new HashSet<>(c.getBrandIds()) : new HashSet<>())
                .productIds(c.getProductIds() != null ? new HashSet<>(c.getProductIds()) : new HashSet<>())
                .excludedProductIds(c.getExcludedProductIds() != null ? new HashSet<>(c.getExcludedProductIds()) : new HashSet<>())
                .productDiscounts(c.getProductDiscounts() != null ? new HashMap<>(c.getProductDiscounts()) : new HashMap<>())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .status(status)
                .build();
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "campaign-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "campaign-" + UUID.randomUUID().toString().substring(0, 8) : slug;
    }

    private record CartProductInfo(Product product, int quantity, boolean hasExchange, BigDecimal lineTotal, BigDecimal exchangeDiscount) {}
}
