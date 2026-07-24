package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    Optional<Product> findProductByProductName(String productName);

    List<Product> findByProductCategoryAndBrand(Category category, Brand brand);

    Optional<Product> findBySeo_Slug(String slug);
}
