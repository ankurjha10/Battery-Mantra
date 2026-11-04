package com.api.batterymantra.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Startup runner to audit and sanitize all legacy canonical URLs and CMS blog links in the DB.
 * Ensures non-www HTTPS canonical URL (https://batterymantra.com).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeoAuditRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting SEO canonical audit and URL normalization check...");
        try {
            int seoPagesUpdated = executeUpdate(
                    "UPDATE seo_pages SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%www.batterymantra.com%'"
            );
            executeUpdate(
                    "UPDATE seo_pages SET seo_canonical_url = REPLACE(seo_canonical_url, 'http://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%www.batterymantra.com%'"
            );
            executeUpdate(
                    "UPDATE seo_pages SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://batterymantra.in', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%batterymantra.in%'"
            );

            int cmsPagesCanonical = executeUpdate(
                    "UPDATE cms_pages SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%www.batterymantra.com%'"
            );
            executeUpdate(
                    "UPDATE cms_pages SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://batterymantra.in', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%batterymantra.in%'"
            );
            int cmsContentUpdated = executeUpdate(
                    "UPDATE cms_pages SET content = REPLACE(content, 'https://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE content LIKE '%www.batterymantra.com%'"
            );
            executeUpdate(
                    "UPDATE cms_pages SET content = REPLACE(content, 'http://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE content LIKE '%www.batterymantra.com%'"
            );
            executeUpdate(
                    "UPDATE cms_pages SET content = REPLACE(content, 'https://batterymantra.in', 'https://batterymantra.com') " +
                    "WHERE content LIKE '%batterymantra.in%'"
            );
            executeUpdate(
                    "UPDATE cms_pages SET content2 = REPLACE(content2, 'https://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE content2 LIKE '%www.batterymantra.com%'"
            );

            int productsUpdated = executeUpdate(
                    "UPDATE products SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%www.batterymantra.com%'"
            );
            executeUpdate(
                    "UPDATE products SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://batterymantra.in', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%batterymantra.in%'"
            );

            executeUpdate(
                    "UPDATE categories SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%www.batterymantra.com%'"
            );
            executeUpdate(
                    "UPDATE brands SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com') " +
                    "WHERE seo_canonical_url LIKE '%www.batterymantra.com%'"
            );

            log.info("SEO canonical audit completed. Modified: [seo_pages: {}, cms_canonical: {}, cms_content: {}, products: {}]",
                    seoPagesUpdated, cmsPagesCanonical, cmsContentUpdated, productsUpdated);
        } catch (Exception e) {
            log.warn("SEO canonical audit notice (table or columns may not be initialized yet): {}", e.getMessage());
        }
    }

    private int executeUpdate(String sql) {
        try {
            return jdbcTemplate.update(sql);
        } catch (Exception e) {
            log.debug("Skipping statement due to: {}", e.getMessage());
            return 0;
        }
    }
}

