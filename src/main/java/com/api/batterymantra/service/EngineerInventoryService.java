package com.api.batterymantra.service;

import com.api.batterymantra.dto.engineer.EngineerInventoryResponse;
import com.api.batterymantra.dto.engineer.LoadUnloadStockRequest;
import com.api.batterymantra.entity.EngineerInventory;
import com.api.batterymantra.entity.EngineerProfile;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.repository.EngineerInventoryRepository;
import com.api.batterymantra.repository.EngineerProfileRepository;
import com.api.batterymantra.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EngineerInventoryService {

    private final EngineerInventoryRepository inventoryRepository;
    private final EngineerProfileRepository engineerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public EngineerInventoryResponse loadStock(LoadUnloadStockRequest request) {
        EngineerProfile engineer = engineerRepository.findById(request.getEngineerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Engineer not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getProductStock() < request.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock in main warehouse");
        }

        // Deduct from main warehouse
        product.setProductStock(product.getProductStock() - request.getQuantity());
        productRepository.save(product);

        // Add to Engineer Inventory
        EngineerInventory inventory = inventoryRepository
                .findByEngineerIdAndProductProductId(request.getEngineerId(), request.getProductId())
                .orElse(EngineerInventory.builder()
                        .engineer(engineer)
                        .product(product)
                        .quantity(0)
                        .build());

        inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
        inventory = inventoryRepository.save(inventory);

        return mapToResponse(inventory);
    }

    @Transactional
    public EngineerInventoryResponse unloadStock(LoadUnloadStockRequest request) {
        EngineerInventory inventory = inventoryRepository
                .findByEngineerIdAndProductProductId(request.getEngineerId(), request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inventory record not found for this engineer and product"));

        if (inventory.getQuantity() < request.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Engineer does not have that much stock to unload");
        }

        Product product = inventory.getProduct();

        // Add back to main warehouse
        product.setProductStock(product.getProductStock() + request.getQuantity());
        productRepository.save(product);

        // Deduct from Engineer Inventory
        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        inventory = inventoryRepository.save(inventory);

        return mapToResponse(inventory);
    }

    public List<EngineerInventoryResponse> getEngineerStock(UUID engineerId) {
        return inventoryRepository.findByEngineerId(engineerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private EngineerInventoryResponse mapToResponse(EngineerInventory inventory) {
        return EngineerInventoryResponse.builder()
                .id(inventory.getId())
                .engineerId(inventory.getEngineer().getId())
                .productId(inventory.getProduct().getProductId())
                .productName(inventory.getProduct().getProductName())
                .quantity(inventory.getQuantity())
                .build();
    }
}
