package com.api.batterymantra.service;

import com.api.batterymantra.dto.cms.CmsPageDto;
import com.api.batterymantra.dto.cms.CreateCmsPageRequest;
import com.api.batterymantra.entity.CmsPage;
import com.api.batterymantra.exception.ResourceNotFoundException;
import com.api.batterymantra.repository.CmsPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsPageService {

    private final CmsPageRepository cmsPageRepository;

    @Transactional(readOnly = true)
    public List<CmsPageDto> getAllPages() {
        return cmsPageRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CmsPageDto getPageById(UUID id) {
        CmsPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CMS Page not found with id: " + id));
        return mapToDto(page);
    }

    @Transactional(readOnly = true)
    public CmsPageDto getPageBySlug(String slug) {
        CmsPage page = cmsPageRepository.findBySeoSlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Active CMS Page not found with slug: " + slug));
        return mapToDto(page);
    }

    @Transactional
    public CmsPageDto createPage(CreateCmsPageRequest request) {
        CmsPage page = new CmsPage();
        page.setTitle(request.getTitle());
        page.setSubTitle(request.getSubTitle());
        page.setImage1(request.getImage1());
        page.setImage2(request.getImage2());
        page.setContent(request.getContent());
        page.setContent2(request.getContent2());
        page.setActive(request.isActive());
        page.setSeo(request.getSeo());

        CmsPage saved = cmsPageRepository.save(page);
        return mapToDto(saved);
    }

    @Transactional
    public CmsPageDto updatePage(UUID id, CreateCmsPageRequest request) {
        CmsPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CMS Page not found with id: " + id));

        page.setTitle(request.getTitle());
        page.setSubTitle(request.getSubTitle());
        page.setImage1(request.getImage1());
        page.setImage2(request.getImage2());
        page.setContent(request.getContent());
        page.setContent2(request.getContent2());
        page.setActive(request.isActive());
        page.setSeo(request.getSeo());

        CmsPage updated = cmsPageRepository.save(page);
        return mapToDto(updated);
    }

    @Transactional
    public void deletePage(UUID id) {
        if (!cmsPageRepository.existsById(id)) {
            throw new ResourceNotFoundException("CMS Page not found with id: " + id);
        }
        cmsPageRepository.deleteById(id);
    }

    private CmsPageDto mapToDto(CmsPage page) {
        return CmsPageDto.builder()
                .pageId(page.getPageId())
                .title(page.getTitle())
                .subTitle(page.getSubTitle())
                .image1(page.getImage1())
                .image2(page.getImage2())
                .content(page.getContent())
                .content2(page.getContent2())
                .isActive(page.isActive())
                .seo(page.getSeo())
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
}
