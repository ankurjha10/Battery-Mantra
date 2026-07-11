package com.api.batterymantra.service;

import com.api.batterymantra.dto.product.CreateProductRequest;
import com.api.batterymantra.dto.product.ProductDetailResponse;
import com.api.batterymantra.dto.product.ProductListResponse;
import com.api.batterymantra.dto.product.UpdateProductRequest;
import com.api.batterymantra.dto.vehicle.VehicleResponse;
import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.Vehicle;
import com.api.batterymantra.repository.BrandRepository;
import com.api.batterymantra.repository.CartItemRepository;
import com.api.batterymantra.repository.CategoryRepository;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.VehicleRepository;
import com.api.batterymantra.repository.specification.ProductSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final VehicleRepository vehicleRepository;
    private final CartItemRepository cartItemRepository;

    private static final String PRODUCT_NOT_FOUND = "Product not found with id: ";

    private ProductListResponse toListResponse(Product p) {
        ProductListResponse res = new ProductListResponse();
        res.setProductId(p.getProductId());
        res.setProductName(p.getProductName());
        res.setProductPrice(p.getProductPrice());
        res.setExchangeDiscount(p.getExchangeDiscount());
        res.setProductImage(p.getProductImage());
        res.setProductCategory(p.getProductCategory().getCategoryName());
        res.setBrandName(p.getBrand() != null ? p.getBrand().getBrandName() : null);
        return res;
    }

    private ProductDetailResponse toDetailResponse(Product p) {
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

        if (p.getBrand() != null) {
            res.setBrandName(p.getBrand().getBrandName());
            res.setBrandId(p.getBrand().getBrandId());
        }

        if (p.getCompatibleVehicle() != null) {
            res.setCompatibleVehicles(p.getCompatibleVehicle().stream().map(v -> {
                VehicleResponse vr = new VehicleResponse();
                vr.setVehicleId(v.getVehicleId());
                vr.setMake(v.getMake());
                vr.setModel(v.getModel());
                vr.setFuelType(v.getFuelType());
                return vr;
            }).toList());
        } else {
            res.setCompatibleVehicles(new ArrayList<>());
        }

        return res;
    }

    @Cacheable(value = "products")
    public List<ProductListResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::toListResponse).toList();
    }

    @Cacheable(value = "products", key = "#id")
    public ProductDetailResponse getProductById(UUID id) {
        return productRepository.findById(id)
                .map(this::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND + id));
    }

    @Cacheable(value = "products", key = "#name")
    public ProductDetailResponse getProductByName(String name) {
        return productRepository.findProductByProductName(name)
                .map(this::toDetailResponse)
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

    /**
     * Dynamic product filtering with Specification pattern.
     * Supports: category, brand, vehicle compatibility, price range, JSONB specs, keyword search.
     */
    public Page<ProductListResponse> filterProducts(UUID categoryId, UUID brandId, UUID vehicleId,
                                                     BigDecimal minPrice, BigDecimal maxPrice,
                                                     String specKey, String specValue,
                                                     String keyword, Pageable pageable) {
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
            spec = spec.and(ProductSpecification.hasCompatibleVehicle(vehicleId));
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

        return productRepository.findAll(spec, pageable).map(this::toListResponse);
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

        // Set compatible vehicles
        if (dto.getCompatibleVehicleIds() != null && !dto.getCompatibleVehicleIds().isEmpty()) {
            List<Vehicle> vehicles = vehicleRepository.findAllById(dto.getCompatibleVehicleIds());
            if (vehicles.size() != dto.getCompatibleVehicleIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "One or more vehicle IDs are invalid");
            }
            product.setCompatibleVehicle(vehicles);
        }

        Product saved = productRepository.save(product);
        return toDetailResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND + id));

        cartItemRepository.deleteByProduct_ProductId(id);
        productRepository.delete(product);
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

        if (dto.getCompatibleVehicleIds() != null) {
            List<Vehicle> vehicles = vehicleRepository.findAllById(dto.getCompatibleVehicleIds());
            product.setCompatibleVehicle(vehicles);
        }

        Product saved = productRepository.save(product);
        return toDetailResponse(saved);
    }
}