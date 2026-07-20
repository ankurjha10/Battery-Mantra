package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    Optional<Product> findProductByProductName(String productName);
    
    java.util.List<Product> findByProductCategoryAndBrand(com.api.batterymantra.entity.Category category, com.api.batterymantra.entity.Brand brand);
}
