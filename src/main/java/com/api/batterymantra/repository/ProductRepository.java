package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Brand;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    Optional<Product> findProductByProductName(String productName);

    List<Product> findByProductCategoryAndBrand(Category category, Brand brand);

    Optional<Product> findBySeo_Slug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :id")
    Optional<Product> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId IN :ids")
    List<Product> findAllByIdWithLock(@Param("ids") Collection<UUID> ids);

    @Query("SELECT COUNT(p) FROM Product p")
    long countTotalSkus();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.productStock > COALESCE(p.lowStockThreshold, 10)")
    long countInStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.productStock > 0 AND p.productStock <= COALESCE(p.lowStockThreshold, 10)")
    long countLowStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.productStock <= 0")
    long countOutOfStock();
}
