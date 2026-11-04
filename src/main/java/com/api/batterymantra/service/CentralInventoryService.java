package com.api.batterymantra.service;

import com.api.batterymantra.dto.inventory.*;
import com.api.batterymantra.entity.*;
import com.api.batterymantra.entity.enums.StockStatus;
import com.api.batterymantra.exception.InsufficientStockException;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.ProductRepository;
import com.api.batterymantra.repository.StockAuditLogRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentralInventoryService {

    private final ProductRepository productRepository;
    private final StockAuditLogRepository stockAuditLogRepository;

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    /**
     * Dashboard view: Summary metrics + Paginated inventory with status filtering.
     */
    @Transactional(readOnly = true)
    public InventoryDashboardResponse getInventoryDashboard(
            StockStatus status, UUID categoryId, UUID brandId, String search, int page, int size) {

        long totalSkus = productRepository.countTotalSkus();
        long inStockCount = productRepository.countInStock();
        long lowStockCount = productRepository.countLowStock();
        long outOfStockCount = productRepository.countOutOfStock();

        InventorySummaryDto summary = InventorySummaryDto.builder()
                .totalSkus(totalSkus)
                .inStockCount(inStockCount)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .lowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "productStock"));
        Specification<Product> spec = buildInventorySpecification(status, categoryId, brandId, search);

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        Page<InventoryItemDto> itemDtos = productPage.map(this::mapToItemDto);

        return InventoryDashboardResponse.builder()
                .summary(summary)
                .items(itemDtos)
                .build();
    }

    /**
     * Inline stock update for a single product.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public UpdateStockResponse updateProductStock(UUID productId, UpdateStockRequest request, String performedBy) {
        if (request.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int oldStock = product.getProductStock();
        int newStock = request.getStockQuantity();
        int change = newStock - oldStock;

        product.setProductStock(newStock);
        productRepository.save(product);

        recordAuditLog(product, "MANUAL_ADJUSTMENT", change, oldStock, newStock, null,
                performedBy != null ? performedBy : "ADMIN", request.getNotes());

        log.info("Manual stock adjustment for product '{}' ({}): {} -> {} by {}",
                product.getProductName(), productId, oldStock, newStock, performedBy);

        return UpdateStockResponse.builder()
                .productId(productId)
                .productStock(newStock)
                .stockStatus(product.getStockStatus())
                .message("Stock updated successfully")
                .build();
    }

    /**
     * Bulk stock update across multiple products.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public BulkStockUpdateResponse bulkUpdateStock(List<BulkStockUpdateRequest> requests, String performedBy) {
        if (requests == null || requests.isEmpty()) {
            return new BulkStockUpdateResponse(0, "No products to update");
        }

        int count = 0;
        for (BulkStockUpdateRequest req : requests) {
            if (req.getStockQuantity() < 0) {
                throw new IllegalArgumentException("Stock quantity cannot be negative for productId: " + req.getProductId());
            }

            Product product = productRepository.findByIdWithLock(req.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + req.getProductId()));

            int oldStock = product.getProductStock();
            int newStock = req.getStockQuantity();
            int change = newStock - oldStock;

            product.setProductStock(newStock);
            productRepository.save(product);

            recordAuditLog(product, "BULK_UPDATE", change, oldStock, newStock, null,
                    performedBy != null ? performedBy : "ADMIN", req.getNotes());
            count++;
        }

        log.info("Bulk stock update executed for {} products by {}", count, performedBy);

        return BulkStockUpdateResponse.builder()
                .updatedCount(count)
                .message("Bulk stock updated successfully")
                .build();
    }

    /**
     * Concurrency-safe stock auto-deduction on order checkout (Pessimistic Lock).
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deductStockForOrder(Orders order, String performedBy) {
        if (order == null || order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        // Check if stock has already been deducted for this order to guarantee idempotency
        if (order.getOrderId() != null && stockAuditLogRepository.existsByOrderIdAndChangeType(order.getOrderId(), "ORDER_DEDUCT")) {
            log.warn("Stock already deducted for order: {}, skipping duplicate deduction.", order.getOrderId());
            return;
        }

        for (OrderItems item : order.getOrderItems()) {
            Product rawProduct = item.getProduct();
            if (rawProduct == null || rawProduct.getProductId() == null) {
                continue;
            }

            // Pessimistic Write Lock (SELECT ... FOR UPDATE)
            Product lockedProduct = productRepository.findByIdWithLock(rawProduct.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + rawProduct.getProductId()));

            int availableStock = lockedProduct.getProductStock();
            int requestedQuantity = item.getQuantity();

            if (availableStock < requestedQuantity) {
                log.error("Insufficient stock for product '{}' ({}): available={}, requested={}",
                        lockedProduct.getProductName(), lockedProduct.getProductId(), availableStock, requestedQuantity);
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + lockedProduct.getProductName()
                                + "'. Available: " + availableStock + ", Requested: " + requestedQuantity,
                        lockedProduct.getProductId(),
                        availableStock
                );
            }

            int newStock = availableStock - requestedQuantity;
            lockedProduct.setProductStock(newStock);
            productRepository.save(lockedProduct);

            recordAuditLog(lockedProduct, "ORDER_DEDUCT", -requestedQuantity, availableStock, newStock,
                    order.getOrderId(), performedBy != null ? performedBy : "SYSTEM",
                    "Order placed: " + order.getOrderId());
        }

        order.setStockRestored(false);
    }

    /**
     * Idempotent stock auto-restoration on order cancellation or return.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void restoreStockForOrder(Orders order, String performedBy, String reason) {
        if (order == null || order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        // Idempotency check: avoid double restock
        if (order.isStockRestored()) {
            log.warn("Stock already marked restored for order: {}, skipping.", order.getOrderId());
            return;
        }

        if (order.getOrderId() != null && stockAuditLogRepository.existsByOrderIdAndChangeType(order.getOrderId(), "ORDER_RESTORE")) {
            log.warn("Stock restore audit log already exists for order: {}, skipping duplicate restore.", order.getOrderId());
            order.setStockRestored(true);
            return;
        }

        for (OrderItems item : order.getOrderItems()) {
            Product rawProduct = item.getProduct();
            if (rawProduct == null || rawProduct.getProductId() == null) {
                continue;
            }

            Product lockedProduct = productRepository.findByIdWithLock(rawProduct.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + rawProduct.getProductId()));

            int oldStock = lockedProduct.getProductStock();
            int newStock = oldStock + item.getQuantity();

            lockedProduct.setProductStock(newStock);
            productRepository.save(lockedProduct);

            recordAuditLog(lockedProduct, "ORDER_RESTORE", item.getQuantity(), oldStock, newStock,
                    order.getOrderId(), performedBy != null ? performedBy : "SYSTEM",
                    reason != null ? reason : "Order cancelled or returned");
        }

        order.setStockRestored(true);
        log.info("Stock restored for cancelled order: {}", order.getOrderId());
    }

    /**
     * Fetch audit log history for a specific product.
     */
    @Transactional(readOnly = true)
    public Page<StockAuditLogResponse> getProductAuditLogs(UUID productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        return stockAuditLogRepository.findByProductProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(log -> StockAuditLogResponse.builder()
                        .id(log.getId())
                        .productId(log.getProduct().getProductId())
                        .productName(log.getProduct().getProductName())
                        .changeType(log.getChangeType())
                        .quantityChange(log.getQuantityChange())
                        .previousStock(log.getPreviousStock())
                        .newStock(log.getNewStock())
                        .orderId(log.getOrderId())
                        .performedBy(log.getPerformedBy())
                        .notes(log.getNotes())
                        .createdAt(log.getCreatedAt())
                        .build());
    }

    /**
     * Record an audit entry in stock_audit_logs.
     */
    public void recordAuditLog(Product product, String changeType, int quantityChange,
                               int previousStock, int newStock, UUID orderId,
                               String performedBy, String notes) {
        StockAuditLog auditLog = StockAuditLog.builder()
                .product(product)
                .changeType(changeType)
                .quantityChange(quantityChange)
                .previousStock(previousStock)
                .newStock(newStock)
                .orderId(orderId)
                .performedBy(performedBy)
                .notes(notes)
                .build();
        stockAuditLogRepository.save(auditLog);
    }

    private InventoryItemDto mapToItemDto(Product p) {
        int threshold = p.getLowStockThreshold() > 0 ? p.getLowStockThreshold() : DEFAULT_LOW_STOCK_THRESHOLD;
        return InventoryItemDto.builder()
                .productId(p.getProductId())
                .productName(p.getProductName())
                .productImage(p.getProductImage())
                .brandName(p.getBrand() != null ? p.getBrand().getBrandName() : null)
                .categoryName(p.getProductCategory() != null ? p.getProductCategory().getCategoryName() : null)
                .capacity(p.getCapacity())
                .productPrice(p.getProductPrice())
                .productStock(p.getProductStock())
                .lowStockThreshold(threshold)
                .stockStatus(p.getStockStatus())
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : (p.getCreatedAt() != null ? p.getCreatedAt() : null))
                .build();
    }

    private Specification<Product> buildInventorySpecification(
            StockStatus status, UUID categoryId, UUID brandId, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && status != StockStatus.ALL) {
                Expression<Integer> threshold = cb.coalesce(root.get("lowStockThreshold"), DEFAULT_LOW_STOCK_THRESHOLD);
                Expression<Integer> stock = root.get("productStock");

                if (status == StockStatus.OUT_OF_STOCK) {
                    predicates.add(cb.lessThanOrEqualTo(stock, 0));
                } else if (status == StockStatus.LOW_STOCK) {
                    predicates.add(cb.and(
                            cb.greaterThan(stock, 0),
                            cb.lessThanOrEqualTo(stock, threshold)
                    ));
                } else if (status == StockStatus.IN_STOCK) {
                    predicates.add(cb.greaterThan(stock, threshold));
                }
            }

            if (categoryId != null) {
                Join<Product, Category> categoryJoin = root.join("categories", JoinType.LEFT);
                Predicate mainCategory = cb.equal(root.get("productCategory").get("categoryId"), categoryId);
                Predicate multiCategory = cb.equal(categoryJoin.get("categoryId"), categoryId);
                predicates.add(cb.or(mainCategory, multiCategory));
                query.distinct(true);
            }

            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("brandId"), brandId));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("productName")), pattern);
                Predicate capacityLike = cb.like(cb.lower(root.get("capacity")), pattern);
                Predicate highlightsLike = cb.like(cb.lower(root.get("highlights")), pattern);
                predicates.add(cb.or(nameLike, capacityLike, highlightsLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

