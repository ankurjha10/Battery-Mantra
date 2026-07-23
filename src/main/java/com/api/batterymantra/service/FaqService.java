package com.api.batterymantra.service;

import com.api.batterymantra.dto.faq.FaqRequest;
import com.api.batterymantra.dto.faq.FaqResponse;
import com.api.batterymantra.entity.Faq;
import com.api.batterymantra.enums.FaqPageType;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    @Transactional
    public FaqResponse createFaq(FaqRequest request) {
        Faq faq = Faq.builder()
                .pageType(request.getPageType())
                .title(request.getTitle())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        faq = faqRepository.save(faq);
        return mapToResponse(faq);
    }

    @Transactional
    public FaqResponse updateFaq(UUID faqId, FaqRequest request) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new ResourceNotFoundException("Faq not found"));

        faq.setPageType(request.getPageType());
        faq.setTitle(request.getTitle());
        faq.setDescription(request.getDescription());
        
        if (request.getIsActive() != null) {
            faq.setActive(request.getIsActive());
        }

        faq = faqRepository.save(faq);
        return mapToResponse(faq);
    }

    @Transactional
    public void deleteFaq(UUID faqId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new ResourceNotFoundException("Faq not found"));
        faqRepository.delete(faq);
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> getAllFaqs() {
        return faqRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> getFaqsByPageType(FaqPageType pageType) {
        return faqRepository.findByPageTypeAndIsActiveTrue(pageType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private FaqResponse mapToResponse(Faq faq) {
        return FaqResponse.builder()
                .faqId(faq.getFaqId())
                .pageType(faq.getPageType())
                .title(faq.getTitle())
                .description(faq.getDescription())
                .isActive(faq.isActive())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
