package com.api.batterymantra.service;

import com.api.batterymantra.entity.SeoPage;
import com.api.batterymantra.entity.SeoTemplate;
import com.api.batterymantra.repository.SeoPageRepository;
import com.api.batterymantra.repository.SeoTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeoService {

    private final SeoPageRepository seoPageRepository;
    private final SeoTemplateRepository seoTemplateRepository;

    public List<SeoPage> getAllSeoPages() {
        return seoPageRepository.findAll();
    }

    public SeoPage getSeoPageByRoute(String route) {
        return seoPageRepository.findByPageRoute(route).orElse(null);
    }

    @Transactional
    public SeoPage saveSeoPage(SeoPage seoPage) {
        if (seoPage.getPageId() != null) {
            SeoPage existing = seoPageRepository.findById(seoPage.getPageId()).orElse(null);
            if (existing != null) {
                existing.setPageName(seoPage.getPageName());
                existing.setPageRoute(seoPage.getPageRoute());
                existing.setSeo(seoPage.getSeo());
                return seoPageRepository.save(existing);
            }
        }
        return seoPageRepository.save(seoPage);
    }

    @Transactional
    public void deleteSeoPage(UUID id) {
        seoPageRepository.deleteById(id);
    }

    public List<SeoTemplate> getAllSeoTemplates() {
        return seoTemplateRepository.findAll();
    }

    public SeoTemplate getSeoTemplateByType(String type) {
        return seoTemplateRepository.findByTemplateType(type).orElse(null);
    }

    @Transactional
    public SeoTemplate saveSeoTemplate(SeoTemplate template) {
        Optional<SeoTemplate> existingOpt = seoTemplateRepository.findByTemplateType(template.getTemplateType());
        if (existingOpt.isPresent()) {
            SeoTemplate existing = existingOpt.get();
            existing.setSeoTitleTemplate(template.getSeoTitleTemplate());
            existing.setSeoDescriptionTemplate(template.getSeoDescriptionTemplate());
            existing.setSeoKeywordsTemplate(template.getSeoKeywordsTemplate());
            existing.setOgTitleTemplate(template.getOgTitleTemplate());
            existing.setOgDescriptionTemplate(template.getOgDescriptionTemplate());
            existing.setShortDescriptionTemplate(template.getShortDescriptionTemplate());
            return seoTemplateRepository.save(existing);
        }
        return seoTemplateRepository.save(template);
    }
}
