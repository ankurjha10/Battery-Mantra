package com.api.batterymantra.service;

import com.api.batterymantra.dto.product.CityPricingDto;
import com.api.batterymantra.dto.product.CreateProductRequest;
import com.api.batterymantra.dto.product.ProductDetailResponse;
import com.api.batterymantra.dto.product.ProductListResponse;
import com.api.batterymantra.dto.product.UpdateProductRequest;
import com.api.batterymantra.dto.vehicle.VehicleResponse;
import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.ProductCityPricing;
import com.api.batterymantra.entity.Vehicle;
import com.api.batterymantra.util.SeoUtil;
import com.api.batterymantra.repository.BrandRepository;
import com.api.batterymantra.repository.CartItemRepository;
import com.api.batterymantra.repository.CategoryRepository;
import com.api.batterymantra.repository.CityRepository;
import com.api.batterymantra.repository.OrderItemRepository;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.VehicleRepository;
import com.api.batterymantra.repository.specification.ProductSpecification;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final VehicleRepository vehicleRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final CityRepository cityRepository;
    private final BulkPricingService bulkPricingService;

    private static final String PRODUCT_NOT_FOUND = "Product not found with id: ";

    private ProductListResponse toListResponse(Product p, UUID cityId) {
        ProductListResponse res = new ProductListResponse();
        res.setProductId(p.getProductId());
        res.setProductName(p.getProductName());
        res.setProductPrice(p.getProductPrice());
        res.setExchangeDiscount(p.getExchangeDiscount());
        res.setProductImage(p.getProductImage());
        res.setProductCategory(p.getProductCategory().getCategoryName());
        res.setBrandName(p.getBrand() != null ? p.getBrand().getBrandName() : null);
        res.setCapacity(p.getCapacity());
        res.setAdditionalImages(p.getAdditionalImages() != null ? new ArrayList<>(p.getAdditionalImages()) : new ArrayList<>());
        res.setAutoAssignToPartner(p.isAutoAssignToPartner());
        res.setApproved(p.isApproved());
        res.setCreatedByPartnerId(p.getCreatedByPartnerId());
        res.setPartnerBusinessName(p.getPartnerBusinessName());

        City currentCity = null;
        if (cityId != null) {
            currentCity = cityRepository.findById(cityId).orElse(null);
        }

        if (cityId != null && p.getCityPrices() != null) {
            p.getCityPrices().stream()
                    .filter(cp -> cp.getCity().getCityId().equals(cityId))
                    .findFirst()
                    .ifPresent(cp -> {
                        res.setProductPrice(cp.getPrice());
                        res.setExchangeDiscount(cp.getExchangeDiscount());
                    });
        }
        
        res.setSeo(SeoUtil.resolveSeo(p.getSeo(), currentCity, p));
        
        return res;
    }

    private ProductDetailResponse toDetailResponse(Product p, UUID cityId) {
        ProductDetailResponse res = new ProductDetailResponse();
        res.setProductId(p.getProductId());
        res.setProductName(p.getProductName());
        res.setProductDescription(p.getProductDescription());
        res.setProductPrice(p.getProductPrice());
        res.setExchangeDiscount(p.getExchangeDiscount());
        res.setProductImage(p.getProductImage());
        res.setProductStock(p.getProductStock());
        res.setCategoryName(p.getProductCategory().getCategoryName());
        res.setCategoryId(p.getProductCategory().getCategoryId());
        res.setSpecs(p.getSpec());
        res.setAdditionalImages(p.getAdditionalImages() != null ? new ArrayList<>(p.getAdditionalImages()) : new ArrayList<>());

        if (p.getBrand() != null) {
            res.setBrandName(p.getBrand().getBrandName());
            res.setBrandId(p.getBrand().getBrandId());
        }

        res.setCapacity(p.getCapacity());
        res.setAutoAssignToPartner(p.isAutoAssignToPartner());
        res.setApproved(p.isApproved());
        res.setCreatedByPartnerId(p.getCreatedByPartnerId());
        res.setPartnerBusinessName(p.getPartnerBusinessName());

        if (p.getCityPrices() != null) {
            res.setCityPrices(p.getCityPrices().stream().map(cp -> {
                CityPricingDto dto = new CityPricingDto();
                dto.setCityId(cp.getCity().getCityId());
                dto.setPrice(cp.getPrice());
                dto.setExchangeDiscount(cp.getExchangeDiscount());
                dto.setStock(cp.getStock());
                return dto;
            }).toList());
        }

        City currentCity = null;
        if (cityId != null) {
            currentCity = cityRepository.findById(cityId).orElse(null);
        }

        if (cityId != null && p.getCityPrices() != null) {
            p.getCityPrices().stream()
                    .filter(cp -> cp.getCity().getCityId().equals(cityId))
                    .findFirst()
                    .ifPresent(cp -> {
                        res.setProductPrice(cp.getPrice());
                        res.setExchangeDiscount(cp.getExchangeDiscount());
                        res.setProductStock(cp.getStock());
                    });
        }
        
        res.setSeo(SeoUtil.resolveSeo(p.getSeo(), currentCity, p));
        
        return res;
    }

    @Cacheable(value = "products", key = "{'all', #cityId}")
    @Transactional(readOnly = true)
    public List<ProductListResponse> getAllProducts(UUID cityId) {
        return productRepository.findAll().stream().map(p -> toListResponse(p, cityId)).toList();
    }

    @Cacheable(value = "products", key = "{#idOrSlug, #cityId}")
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductByIdOrSlug(String idOrSlug, UUID cityId) {
        Optional<Product> productOpt;
        try {
            UUID id = UUID.fromString(idOrSlug);
            productOpt = productRepository.findById(id);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try finding by slug
            productOpt = productRepository.findBySeo_Slug(idOrSlug);
        }

        return productOpt
                .map(p -> toDetailResponse(p, cityId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND + idOrSlug));
    }

    @Cacheable(value = "products", key = "{#name, #cityId}")
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductByName(String name, UUID cityId) {
        return productRepository.findProductByProductName(name)
                .map(p -> toDetailResponse(p, cityId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found with name: " + name));
    }

    private void collectCategoryIds(Category category, List<UUID> ids) {
        if (category == null) return;
        ids.add(category.getCategoryId());
        if (category.getSubCategories() != null) {
            for (Category child : category.getSubCategories()) {
                collectCategoryIds(child, ids);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductListResponse> filterProducts(UUID categoryId, UUID brandId, UUID vehicleId,
                                                     BigDecimal minPrice, BigDecimal maxPrice,
                                                     String specKey, String specValue,
                                                     String keyword, Pageable pageable, UUID cityId) {
        Specification<Product> spec = Specification.where(null);

        if (categoryId != null) {
            List<UUID> categoryIds = new ArrayList<>();
            categoryRepository.findById(categoryId).ifPresent(cat -> {
                collectCategoryIds(cat, categoryIds);
            });
            if (!categoryIds.isEmpty()) {
                spec = spec.and(ProductSpecification.hasCategoryIdIn(categoryIds));
            } else {
                spec = spec.and(ProductSpecification.hasCategoryId(categoryId));
            }
        }
        if (brandId != null) {
            spec = spec.and(ProductSpecification.hasBrandId(brandId));
        }
        if (vehicleId != null) {
            List<String> vehicleCapacities = new ArrayList<>();
            vehicleRepository.findById(vehicleId).ifPresent(v -> {
                if (v.getCapacity() != null && !v.getCapacity().isBlank()) {
                    vehicleCapacities.addAll(java.util.Arrays.stream(v.getCapacity().split(","))
                            .map(String::trim)
                            .filter(c -> !c.isEmpty())
                            .toList());
                }
            });
            if (!vehicleCapacities.isEmpty()) {
                spec = spec.and(ProductSpecification.hasCapacityIn(vehicleCapacities));
            } else {
                spec = spec.and((root, query, cb) -> cb.disjunction()); // Return empty if vehicle has no capacity
            }
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));
        }
        if (specKey != null && specValue != null) {
            spec = spec.and(ProductSpecification.hasSpec(specKey, specValue));
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ProductSpecification.hasNameContaining(keyword));
        }

        return productRepository.findAll(spec, pageable).map(p -> toListResponse(p, cityId));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse addProduct(CreateProductRequest dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found with id: " + dto.getCategoryId()));

        Product product = new Product();
        product.setProductName(dto.getProductName());
        product.setProductDescription(dto.getProductDescription());
        product.setProductImage(dto.getProductImage());
        product.setProductPrice(dto.getProductPrice());
        product.setProductStock(dto.getProductStock());
        if (dto.getExchangeDiscount() != null) {
            product.setExchangeDiscount(dto.getExchangeDiscount());
        }
        product.setProductCategory(category);
        
        if (dto.getAdditionalImages() != null) {
            product.setAdditionalImages(dto.getAdditionalImages());
        }

        // Set brand
        if (dto.getBrandId() != null) {
            Brand brand = brandRepository.findById(dto.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Brand not found with id: " + dto.getBrandId()));
            product.setBrand(brand);
        }

        // Set specs
        if (dto.getSpecs() != null) {
            product.setSpec(dto.getSpecs());
        }

        // Set capacity
        if (dto.getCapacity() != null) {
            product.setCapacity(dto.getCapacity());
        }

        if (dto.getIsAutoAssignToPartner() != null) {
            product.setAutoAssignToPartner(dto.getIsAutoAssignToPartner());
        }

        if (dto.getSeo() != null) {
            product.setSeo(dto.getSeo());
        }

        if (dto.getCityPrices() != null) {
            for (CityPricingDto cpd : dto.getCityPrices()) {
                City city = cityRepository.findById(cpd.getCityId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found with id " + cpd.getCityId()));
                ProductCityPricing pricing = new ProductCityPricing();
                pricing.setProduct(product);
                pricing.setCity(city);
                pricing.setPrice(cpd.getPrice());
                pricing.setExchangeDiscount(cpd.getExchangeDiscount() != null ? cpd.getExchangeDiscount() : BigDecimal.ZERO);
                pricing.setStock(cpd.getStock());
                product.getCityPrices().add(pricing);
            }
        }

        bulkPricingService.applyBulkPricingToProduct(product);
        Product saved = productRepository.save(product);
        return toDetailResponse(saved, null);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND + id));

        // Check if product is referenced by any order items
        if (orderItemRepository.existsByProduct_ProductId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete: Product is referenced in existing orders. Consider deactivating it instead.");
        }

        try {
            cartItemRepository.deleteByProduct_ProductId(id);
            productRepository.delete(product);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete: Product is still referenced by other records.");
        }
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse updateProduct(UUID id, UpdateProductRequest dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND + id));

        if (dto.getProductName() != null)
            product.setProductName(dto.getProductName());

        if (dto.getProductDescription() != null)
            product.setProductDescription(dto.getProductDescription());

        if (dto.getProductImage() != null)
            product.setProductImage(dto.getProductImage());

        if (dto.getProductStock() != null)
            product.setProductStock(dto.getProductStock());

        if (dto.getProductPrice() != null)
            product.setProductPrice(dto.getProductPrice());

        if (dto.getExchangeDiscount() != null)
            product.setExchangeDiscount(dto.getExchangeDiscount());

        if (dto.getAdditionalImages() != null)
            product.setAdditionalImages(dto.getAdditionalImages());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Category not found with id: " + dto.getCategoryId()));
            product.setProductCategory(category);
        }

        if (dto.getBrandId() != null) {
            Brand brand = brandRepository.findById(dto.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Brand not found with id: " + dto.getBrandId()));
            product.setBrand(brand);
        }

        if (dto.getSpecs() != null) {
            product.setSpec(dto.getSpecs());
        }

        if (dto.getCapacity() != null) {
            product.setCapacity(dto.getCapacity());
        }

        if (dto.getIsAutoAssignToPartner() != null) {
            product.setAutoAssignToPartner(dto.getIsAutoAssignToPartner());
        }

        if (dto.getSeo() != null) {
            product.setSeo(dto.getSeo());
        }

        if (dto.getCityPrices() != null) {
            product.getCityPrices().clear();
            for (CityPricingDto cpd : dto.getCityPrices()) {
                City city = cityRepository.findById(cpd.getCityId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found with id " + cpd.getCityId()));
                ProductCityPricing pricing = new ProductCityPricing();
                pricing.setProduct(product);
                pricing.setCity(city);
                pricing.setPrice(cpd.getPrice());
                pricing.setExchangeDiscount(cpd.getExchangeDiscount() != null ? cpd.getExchangeDiscount() : BigDecimal.ZERO);
                pricing.setStock(cpd.getStock());
                product.getCityPrices().add(pricing);
            }
        }

        bulkPricingService.applyBulkPricingToProduct(product);
        Product updatedProduct = productRepository.save(product);
        return toDetailResponse(updatedProduct, null);
    }

    @Transactional
    public ProductDetailResponse addProductByPartner(CreateProductRequest productDto, com.api.batterymantra.entity.PartnerProfile partnerProfile) {
        ProductDetailResponse response = addProduct(productDto);
        Product product = productRepository.findById(response.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND + response.getProductId()));
        product.setApproved(false);
        product.setCreatedByPartnerId(partnerProfile.getId());
        product.setPartnerBusinessName(partnerProfile.getBusinessName());
        Product saved = productRepository.save(product);
        return toDetailResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public List<ProductListResponse> getPendingApprovalProducts() {
        return productRepository.findAll().stream()
                .filter(p -> !p.isApproved())
                .map(p -> toListResponse(p, null))
                .toList();
    }

    @Transactional
    public ProductDetailResponse approveProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND + productId));
        product.setApproved(true);
        Product saved = productRepository.save(product);
        return toDetailResponse(saved, null);
    }

    @Transactional
    public ProductDetailResponse updateCityPricingByPartner(UUID productId, CityPricingDto dto, com.api.batterymantra.entity.PartnerProfile partnerProfile) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND + productId));

        if (partnerProfile.getOperatingCities() == null || partnerProfile.getOperatingCities().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No operating cities assigned to your partner profile. Contact Admin.");
        }

        if (dto.getCityId() == null) {
            dto.setCityId(partnerProfile.getOperatingCities().iterator().next().getCityId());
        }

        // Validate that cityId belongs to partner's operating cities
        boolean isPartnerCity = partnerProfile.getOperatingCities().stream()
                .anyMatch(c -> c.getCityId().equals(dto.getCityId()));

        if (!isPartnerCity) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only change prices for your branch operating cities.");
        }

        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + dto.getCityId()));

        ProductCityPricing existingPricing = product.getCityPrices().stream()
                .filter(cp -> cp.getCity().getCityId().equals(dto.getCityId()))
                .findFirst()
                .orElse(null);

        if (existingPricing != null) {
            existingPricing.setPrice(dto.getPrice());
            if (dto.getExchangeDiscount() != null) existingPricing.setExchangeDiscount(dto.getExchangeDiscount());
            existingPricing.setStock(dto.getStock());
        } else {
            ProductCityPricing pricing = new ProductCityPricing();
            pricing.setProduct(product);
            pricing.setCity(city);
            pricing.setPrice(dto.getPrice());
            pricing.setExchangeDiscount(dto.getExchangeDiscount() != null ? dto.getExchangeDiscount() : BigDecimal.ZERO);
            pricing.setStock(dto.getStock());
            product.getCityPrices().add(pricing);
        }

        Product saved = productRepository.save(product);
        return toDetailResponse(saved, dto.getCityId());
    }
}