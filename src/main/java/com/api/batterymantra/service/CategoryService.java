package com.api.batterymantra.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.api.batterymantra.dto.category.CategoryDetailResponse;
import com.api.batterymantra.dto.category.CategoryListResponse;
import com.api.batterymantra.dto.category.CreateCategoryRequest;
import com.api.batterymantra.dto.category.UpdateCategoryRequest;
import com.api.batterymantra.entity.Category;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private CategoryListResponse toListResponse(Category c) {
        CategoryListResponse res = new CategoryListResponse();
        res.categoryId = c.getCategoryId();
        res.categoryName = c.getCategoryName();
        res.categoryDescription = c.getCategoryDescription();
        res.iconUrl = c.getIconUrl();
        res.displayOrder = c.getDisplayOrder();
        res.parentId = c.getParent() != null ? c.getParent().getCategoryId() : null;
        if (c.getSubCategories() != null) {
            res.subCategories = c.getSubCategories().stream()
                    .map(this::toListResponse)
                    .toList();
        } else {
            res.subCategories = new ArrayList<>();
        }
        return res;
    }

    private CategoryDetailResponse toDetailResponse(Category c) {
        CategoryDetailResponse res = new CategoryDetailResponse();
        res.categoryId = c.getCategoryId();
        res.categoryName = c.getCategoryName();
        res.categoryDescription = c.getCategoryDescription();
        res.iconUrl = c.getIconUrl();
        res.displayOrder = c.getDisplayOrder();
        res.parentId = c.getParent() != null ? c.getParent().getCategoryId() : null;

        if (c.getSubCategories() != null) {
            res.subCategories = c.getSubCategories().stream()
                    .map(this::toListResponse)
                    .toList();
        } else {
            res.subCategories = new ArrayList<>();
        }

        if (c.getProducts() != null) {
            res.products = c.getProducts().stream()
                    .map(Product::getProductId)
                    .toList();
        } else {
            res.products = new ArrayList<>();
        }
        return res;
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryListResponse addCategory(CreateCategoryRequest dto) {
        Category category = new Category();
        category.setCategoryName(dto.categoryName);
        category.setCategoryDescription(dto.categoryDescription);
        category.setIconUrl(dto.iconUrl);
        category.setDisplayOrder(dto.displayOrder);

        if (dto.parentId != null) {
            Category parent = categoryRepository.findById(dto.parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Parent category not found with id: " + dto.parentId));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return toListResponse(saved);
    }

    @Cacheable(value = "categories", key = "#id", condition = "#id != null")
    public CategoryDetailResponse getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .stream()
                .findFirst()
                .map(this::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found with id: " + id));
    }

    @Cacheable(value = "categories", key = "#name", condition = "#name != null")
    public CategoryDetailResponse getCategoryByName(String name) {
        return categoryRepository.findCategoriesByCategoryName(name)
                .stream()
                .findFirst()
                .map(this::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found with name: " + name));
    }

    @Cacheable(value = "categories")
    public List<CategoryListResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toListResponse).toList();
    }

    public List<CategoryListResponse> getRootCategories() {
        return categoryRepository.findByParentIsNullOrderByDisplayOrderAsc()
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found with id: " + id));
        categoryRepository.delete(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryListResponse updateCategory(UUID id, UpdateCategoryRequest dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found with id: " + id));

        if (dto.categoryName != null)
            category.setCategoryName(dto.categoryName);

        if (dto.categoryDescription != null)
            category.setCategoryDescription(dto.categoryDescription);

        if (dto.iconUrl != null)
            category.setIconUrl(dto.iconUrl);

        if (dto.displayOrder != null)
            category.setDisplayOrder(dto.displayOrder);

        if (dto.parentId != null) {
            Category parent = categoryRepository.findById(dto.parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Parent category not found with id: " + dto.parentId));
            category.setParent(parent);
        }

        categoryRepository.save(category);
        return toListResponse(category);
    }
}
