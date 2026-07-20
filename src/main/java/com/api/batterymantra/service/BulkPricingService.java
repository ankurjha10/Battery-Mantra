package com.api.batterymantra.service;

import com.api.batterymantra.dto.BulkPricingRequest;
import com.api.batterymantra.dto.BulkPricingResponse;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.repository.*;
import com.api.batterymantra.util.PricingUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulkPricingService {

    private final BulkPricingMatrixRepository matrixRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CityRepository cityRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<BulkPricingResponse> getMatrix(UUID categoryId, UUID brandId) {
        return matrixRepository.findByCategory_CategoryIdAndBrand_BrandId(categoryId, brandId)
                .stream()
                .map(matrix -> {
                    BulkPricingResponse response = new BulkPricingResponse();
                    response.setCategoryId(matrix.getCategory().getCategoryId());
                    response.setBrandId(matrix.getBrand().getBrandId());
                    response.setCityId(matrix.getCity().getCityId());
                    response.setPercentage(matrix.getPercentage());
                    return response;
                }).collect(Collectors.toList());
    }

    @Transactional
    public BulkPricingResponse updateMatrix(BulkPricingRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found"));

        BulkPricingMatrix matrix = matrixRepository
                .findByCategory_CategoryIdAndBrand_BrandIdAndCity_CityId(request.getCategoryId(), request.getBrandId(), request.getCityId())
                .orElse(new BulkPricingMatrix());

        matrix.setCategory(category);
        matrix.setBrand(brand);
        matrix.setCity(city);
        matrix.setPercentage(request.getPercentage());
        
        matrixRepository.save(matrix);

        // Update all products matching this category and brand
        updateProductsPricing(category, brand, city, request.getPercentage());

        BulkPricingResponse response = new BulkPricingResponse();
        response.setCategoryId(category.getCategoryId());
        response.setBrandId(brand.getBrandId());
        response.setCityId(city.getCityId());
        response.setPercentage(request.getPercentage());
        return response;
    }

    private void updateProductsPricing(Category category, Brand brand, City city, BigDecimal percentage) {
        List<Product> products = productRepository.findByProductCategoryAndBrand(category, brand);
        
        for (Product product : products) {
            if (product.getProductPrice() == null) continue;
            
            // Calculate new price
            BigDecimal calculated = product.getProductPrice().multiply(percentage).divide(BigDecimal.valueOf(100));
            BigDecimal newPrice = PricingUtility.applyRetailRounding(calculated);

            // Find existing city pricing or create new
            Optional<ProductCityPricing> existingPricingOpt = product.getCityPrices().stream()
                    .filter(cp -> cp.getCity().getCityId().equals(city.getCityId()))
                    .findFirst();

            if (existingPricingOpt.isPresent()) {
                existingPricingOpt.get().setPrice(newPrice);
            } else {
                ProductCityPricing newPricing = new ProductCityPricing();
                newPricing.setProduct(product);
                newPricing.setCity(city);
                newPricing.setPrice(newPrice);
                newPricing.setExchangeDiscount(product.getExchangeDiscount() != null ? product.getExchangeDiscount() : BigDecimal.ZERO);
                newPricing.setStock(product.getProductStock()); // default to product stock
                product.getCityPrices().add(newPricing);
            }
        }
        productRepository.saveAll(products);
    }
    
    /**
     * Applies the bulk pricing rules to a newly created or updated product.
     * This ensures the product gets the regional prices automatically.
     */
    public void applyBulkPricingToProduct(Product product) {
        if (product.getProductCategory() == null || product.getBrand() == null || product.getProductPrice() == null) {
            return;
        }
        
        List<BulkPricingMatrix> matrices = matrixRepository
                .findByCategory_CategoryIdAndBrand_BrandId(product.getProductCategory().getCategoryId(), product.getBrand().getBrandId());
                
        if (matrices.isEmpty()) return;
        
        for (BulkPricingMatrix matrix : matrices) {
            BigDecimal calculated = product.getProductPrice().multiply(matrix.getPercentage()).divide(BigDecimal.valueOf(100));
            BigDecimal newPrice = PricingUtility.applyRetailRounding(calculated);
            
            Optional<ProductCityPricing> existingPricingOpt = product.getCityPrices().stream()
                    .filter(cp -> cp.getCity().getCityId().equals(matrix.getCity().getCityId()))
                    .findFirst();

            if (existingPricingOpt.isPresent()) {
                // If it exists, should we overwrite it? Yes, matrix takes precedence, or if user manually supplied it, we might want to keep it?
                // For simplicity, we overwrite it. But wait, if user provided specific prices in the DTO, they might be overwritten.
                // We'll only set it if the user didn't explicitly set a different price?
                // Actually, if the matrix exists, it represents the rule. We apply it.
                existingPricingOpt.get().setPrice(newPrice);
            } else {
                ProductCityPricing newPricing = new ProductCityPricing();
                newPricing.setProduct(product);
                newPricing.setCity(matrix.getCity());
                newPricing.setPrice(newPrice);
                newPricing.setExchangeDiscount(product.getExchangeDiscount() != null ? product.getExchangeDiscount() : BigDecimal.ZERO);
                newPricing.setStock(product.getProductStock());
                product.getCityPrices().add(newPricing);
            }
        }
    }
}
