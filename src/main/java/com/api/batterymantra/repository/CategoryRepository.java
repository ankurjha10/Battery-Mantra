package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>{
    Optional<Category> findCategoriesByCategoryName(String categoryName);
    List<Category> findByParentIsNullOrderByDisplayOrderAsc();
}
