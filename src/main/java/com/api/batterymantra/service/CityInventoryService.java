package com.api.batterymantra.service;

import com.api.batterymantra.dto.inventory.CityInventoryBulkUpdateResponse;
import com.api.batterymantra.dto.inventory.CityProductStockItem;
import com.api.batterymantra.dto.inventory.PartnerBulkInventoryUpdateRequest;
import com.api.batterymantra.dto.inventory.PartnerInventoryItemResponse;
import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.PartnerProfile;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.ProductCityPricing;
import com.api.batterymantra.repository.CityRepository;
import com.api.batterymantra.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CityInventoryService {

    private final ProductRepository productRepository;
    private final CityRepository cityRepository;

    /**
     * Validates that the partner has authorized access to the requested city.
     * Throws 403 Forbidden if cityId is not in the partner's operating cities list.
     */
    public void validatePartnerCityAccess(PartnerProfile partnerProfile, UUID cityId) {
        if (partnerProfile == null || partnerProfile.getOperatingCities() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: You are not authorized to manage inventory for this city");
        }
        boolean hasAccess = partnerProfile.getOperatingCities().stream()
                .anyMatch(c -> c.getCityId() != null && c.getCityId().equals(cityId));
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: You are not authorized to manage inventory for this city");
        }
    }

    /**
     * GET /api/partner/inventory?cityId={cityId}
     * Returns products with city-specific pricing and local branch stock.
     */
    @Transactional(readOnly = true)
    public List<PartnerInventoryItemResponse> getPartnerCityInventory(UUID cityId, PartnerProfile partnerProfile) {
        validatePartnerCityAccess(partnerProfile, cityId);

        cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + cityId));

        return productRepository.findAll().stream()
                .filter(Product::isApproved)
                .map(p -> toPartnerInventoryItem(p, cityId))
                .toList();
    }

    /**
     * GET /api/admin/inventory/cities/{cityId}
     * Unrestricted city inventory access for Super Admin.
     */
    @Transactional(readOnly = true)
    public List<PartnerInventoryItemResponse> getCityInventoryForAdmin(UUID cityId) {
        cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + cityId));

        return productRepository.findAll().stream()
                .filter(Product::isApproved)
                .map(p -> toPartnerInventoryItem(p, cityId))
                .toList();
    }

    /**
     * PATCH /api/partner/inventory/{productId}/stock?cityId={cityId}&stock={stock}
     * Updates branch stock for a single product with partner city isolation check.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public PartnerInventoryItemResponse updatePartnerProductStock(
            UUID productId, UUID cityId, int stock, PartnerProfile partnerProfile) {

        validatePartnerCityAccess(partnerProfile, cityId);
        if (stock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + cityId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        updateOrCreateCityPricingStock(product, city, stock);
        Product saved = productRepository.save(product);

        log.info("Partner '{}' updated branch stock for product '{}' in city '{}' to {}",
                partnerProfile.getBusinessName(), product.getProductName(), city.getCityName(), stock);

        return toPartnerInventoryItem(saved, cityId);
    }

    /**
     * PATCH /api/admin/inventory/cities/{cityId}/products/{productId}
     * Unrestricted single product city stock update for Super Admin.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public PartnerInventoryItemResponse updateAdminCityProductStock(
            UUID cityId, UUID productId, int stock) {

        if (stock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + cityId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        updateOrCreateCityPricingStock(product, city, stock);
        Product saved = productRepository.save(product);

        log.info("Admin updated city stock for product '{}' in city '{}' to {}",
                product.getProductName(), city.getCityName(), stock);

        return toPartnerInventoryItem(saved, cityId);
    }

    /**
     * POST /api/partner/inventory/bulk-update
     * Bulk updates branch stock across multiple products with partner city isolation check.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public CityInventoryBulkUpdateResponse bulkUpdatePartnerCityStock(
            PartnerBulkInventoryUpdateRequest request, PartnerProfile partnerProfile) {

        validatePartnerCityAccess(partnerProfile, request.getCityId());

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + request.getCityId()));

        int count = 0;
        for (CityProductStockItem item : request.getUpdates()) {
            if (item.getStock() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative for productId: " + item.getProductId());
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + item.getProductId()));

            updateOrCreateCityPricingStock(product, city, item.getStock());
            productRepository.save(product);
            count++;
        }

        log.info("Partner '{}' bulk updated {} products branch stock for city '{}'",
                partnerProfile.getBusinessName(), count, city.getCityName());

        return CityInventoryBulkUpdateResponse.builder()
                .cityId(request.getCityId())
                .updatedCount(count)
                .message("Branch inventory updated successfully")
                .build();
    }

    /**
     * PUT /api/admin/inventory/cities/{cityId}/bulk-update
     * Unrestricted bulk city stock updater for Super Admin.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public CityInventoryBulkUpdateResponse bulkUpdateAdminCityStock(
            UUID cityId, List<CityProductStockItem> updates) {

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + cityId));

        if (updates == null || updates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Updates list cannot be empty");
        }

        int count = 0;
        for (CityProductStockItem item : updates) {
            if (item.getStock() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative for productId: " + item.getProductId());
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + item.getProductId()));

            updateOrCreateCityPricingStock(product, city, item.getStock());
            productRepository.save(product);
            count++;
        }

        log.info("Admin bulk updated {} products city stock for city '{}'", count, city.getCityName());

        return CityInventoryBulkUpdateResponse.builder()
                .cityId(cityId)
                .updatedCount(count)
                .message("City inventory bulk updated successfully")
                .build();
    }

    private void updateOrCreateCityPricingStock(Product product, City city, int stock) {
        if (product.getCityPrices() == null) {
            product.setCityPrices(new ArrayList<>());
        }

        ProductCityPricing existingPricing = product.getCityPrices().stream()
                .filter(cp -> cp.getCity() != null && cp.getCity().getCityId().equals(city.getCityId()))
                .findFirst()
                .orElse(null);

        if (existingPricing != null) {
            existingPricing.setStock(stock);
        } else {
            ProductCityPricing pricing = new ProductCityPricing();
            pricing.setProduct(product);
            pricing.setCity(city);
            pricing.setPrice(product.getProductPrice());
            pricing.setExchangeDiscount(product.getExchangeDiscount() != null ? product.getExchangeDiscount() : BigDecimal.ZERO);
            pricing.setStock(stock);
            product.getCityPrices().add(pricing);
        }
    }

    private PartnerInventoryItemResponse toPartnerInventoryItem(Product p, UUID cityId) {
        ProductCityPricing cityPricing = null;
        if (p.getCityPrices() != null && cityId != null) {
            cityPricing = p.getCityPrices().stream()
                    .filter(cp -> cp.getCity() != null && cp.getCity().getCityId().equals(cityId))
                    .findFirst()
                    .orElse(null);
        }

        BigDecimal price = cityPricing != null && cityPricing.getPrice() != null
                ? cityPricing.getPrice()
                : p.getProductPrice();

        BigDecimal exchangeDiscount = cityPricing != null && cityPricing.getExchangeDiscount() != null
                ? cityPricing.getExchangeDiscount()
                : (p.getExchangeDiscount() != null ? p.getExchangeDiscount() : BigDecimal.ZERO);

        Integer branchStock = cityPricing != null ? cityPricing.getStock() : 0;

        return PartnerInventoryItemResponse.builder()
                .productId(p.getProductId())
                .productName(p.getProductName())
                .productPrice(price)
                .exchangeDiscount(exchangeDiscount)
                .productStock(branchStock)
                .brandName(p.getBrand() != null ? p.getBrand().getBrandName() : null)
                .capacity(p.getCapacity())
                .productCategory(p.getProductCategory() != null ? p.getProductCategory().getCategoryName() : null)
                .productImage(p.getProductImage())
                .build();
    }
}

