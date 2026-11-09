package com.api.batterymantra.service;

import com.api.batterymantra.dto.product.CityPricingRequestDto;
import com.api.batterymantra.dto.product.CityPricingRequestResponse;
import com.api.batterymantra.entity.City;
import com.api.batterymantra.entity.CityPricingRequest;
import com.api.batterymantra.entity.PartnerProfile;
import com.api.batterymantra.entity.Product;
import com.api.batterymantra.entity.ProductCityPricing;
import com.api.batterymantra.entity.enums.PricingRequestStatus;
import com.api.batterymantra.repository.CityPricingRequestRepository;
import com.api.batterymantra.repository.CityRepository;
import com.api.batterymantra.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CityPricingRequestService {

    private final CityPricingRequestRepository cityPricingRequestRepository;
    private final ProductRepository productRepository;
    private final CityRepository cityRepository;

    @Transactional
    public CityPricingRequestResponse createPricingRequest(
            UUID productId,
            CityPricingRequestDto dto,
            PartnerProfile partnerProfile) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + dto.getCityId()));

        boolean operatesInCity = partnerProfile.getOperatingCities() != null &&
                partnerProfile.getOperatingCities().stream().anyMatch(c -> c.getCityId().equals(dto.getCityId()));

        if (!operatesInCity) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only request price changes for your branch operating cities.");
        }

        CityPricingRequest request = CityPricingRequest.builder()
                .product(product)
                .city(city)
                .partner(partnerProfile)
                .price(dto.getPrice())
                .exchangeDiscount(dto.getExchangeDiscount() != null ? dto.getExchangeDiscount() : BigDecimal.ZERO)
                .stock(dto.getStock())
                .reason(dto.getReason())
                .status(PricingRequestStatus.PENDING_APPROVAL)
                .build();

        CityPricingRequest saved = cityPricingRequestRepository.save(request);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CityPricingRequestResponse> getPendingRequestsForPartner(UUID partnerProfileId) {
        return cityPricingRequestRepository
                .findByPartner_IdAndStatusOrderByCreatedAtDesc(partnerProfileId, PricingRequestStatus.PENDING_APPROVAL)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public CityPricingRequestResponse approvePricingRequest(UUID requestId, String adminUsername) {
        CityPricingRequest request = cityPricingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "City pricing request not found: " + requestId));

        if (request.getStatus() != PricingRequestStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pricing request has already been processed with status: " + request.getStatus());
        }

        Product product = request.getProduct();
        City city = request.getCity();

        // Apply requested city pricing to the live catalog
        ProductCityPricing existingPricing = product.getCityPrices().stream()
                .filter(cp -> cp.getCity() != null && cp.getCity().getCityId().equals(city.getCityId()))
                .findFirst()
                .orElse(null);

        if (existingPricing != null) {
            existingPricing.setPrice(request.getPrice());
            if (request.getExchangeDiscount() != null) {
                existingPricing.setExchangeDiscount(request.getExchangeDiscount());
            }
            if (request.getStock() != null) {
                existingPricing.setStock(request.getStock());
            }
        } else {
            ProductCityPricing pricing = new ProductCityPricing();
            pricing.setProduct(product);
            pricing.setCity(city);
            pricing.setPrice(request.getPrice());
            pricing.setExchangeDiscount(request.getExchangeDiscount() != null ? request.getExchangeDiscount() : BigDecimal.ZERO);
            pricing.setStock(request.getStock() != null ? request.getStock() : 0);
            product.getCityPrices().add(pricing);
        }

        productRepository.save(product);

        request.setStatus(PricingRequestStatus.APPROVED);
        request.setReviewedBy(adminUsername != null ? adminUsername : "ADMIN");
        request.setReviewedAt(LocalDateTime.now());
        CityPricingRequest updated = cityPricingRequestRepository.save(request);

        return mapToResponse(updated);
    }

    @Transactional
    public CityPricingRequestResponse rejectPricingRequest(UUID requestId, String reason, String adminUsername) {
        CityPricingRequest request = cityPricingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "City pricing request not found: " + requestId));

        if (request.getStatus() != PricingRequestStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pricing request has already been processed with status: " + request.getStatus());
        }

        request.setStatus(PricingRequestStatus.REJECTED);
        request.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Rejected by Admin");
        request.setReviewedBy(adminUsername != null ? adminUsername : "ADMIN");
        request.setReviewedAt(LocalDateTime.now());
        CityPricingRequest updated = cityPricingRequestRepository.save(request);

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<CityPricingRequestResponse> getPricingRequests(PricingRequestStatus status) {
        List<CityPricingRequest> requests = (status != null)
                ? cityPricingRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                : cityPricingRequestRepository.findAllByOrderByCreatedAtDesc();

        return requests.stream().map(this::mapToResponse).toList();
    }

    private CityPricingRequestResponse mapToResponse(CityPricingRequest entity) {
        return CityPricingRequestResponse.builder()
                .requestId(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getProductId() : null)
                .productName(entity.getProduct() != null ? entity.getProduct().getProductName() : null)
                .cityId(entity.getCity() != null ? entity.getCity().getCityId() : null)
                .cityName(entity.getCity() != null ? entity.getCity().getCityName() : null)
                .partnerId(entity.getPartner() != null ? entity.getPartner().getId() : null)
                .partnerBusinessName(entity.getPartner() != null ? entity.getPartner().getBusinessName() : null)
                .price(entity.getPrice())
                .exchangeDiscount(entity.getExchangeDiscount())
                .stock(entity.getStock())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .rejectionReason(entity.getRejectionReason())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
