-- ==============================================================================
-- Migration: SEO Canonical Audit & Normalization
-- Fixes Google Search Console: "Duplicate without user-selected canonical"
-- Replaces legacy 'www.batterymantra.com' and 'batterymantra.in' with 'https://batterymantra.com'
-- ==============================================================================

-- 1. seo_pages table
UPDATE seo_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.com%';

UPDATE seo_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'http://www.batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.com%';

UPDATE seo_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'http://batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE 'http://batterymantra.com%';

UPDATE seo_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://batterymantra.in', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%batterymantra.in%';

UPDATE seo_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'http://batterymantra.in', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%batterymantra.in%';

UPDATE seo_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.in', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.in%';

-- 2. cms_pages table (canonical URL and blog content links)
UPDATE cms_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.com%';

UPDATE cms_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'http://www.batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.com%';

UPDATE cms_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'http://batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE 'http://batterymantra.com%';

UPDATE cms_pages
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://batterymantra.in', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%batterymantra.in%';

UPDATE cms_pages
SET content = REPLACE(content, 'https://www.batterymantra.com', 'https://batterymantra.com')
WHERE content LIKE '%www.batterymantra.com%';

UPDATE cms_pages
SET content = REPLACE(content, 'http://www.batterymantra.com', 'https://batterymantra.com')
WHERE content LIKE '%www.batterymantra.com%';

UPDATE cms_pages
SET content = REPLACE(content, 'http://batterymantra.com', 'https://batterymantra.com')
WHERE content LIKE '%http://batterymantra.com%';

UPDATE cms_pages
SET content = REPLACE(content, 'https://batterymantra.in', 'https://batterymantra.com')
WHERE content LIKE '%batterymantra.in%';

UPDATE cms_pages
SET content = REPLACE(content, 'http://batterymantra.in', 'https://batterymantra.com')
WHERE content LIKE '%batterymantra.in%';

UPDATE cms_pages
SET content2 = REPLACE(content2, 'https://www.batterymantra.com', 'https://batterymantra.com')
WHERE content2 LIKE '%www.batterymantra.com%';

UPDATE cms_pages
SET content2 = REPLACE(content2, 'http://www.batterymantra.com', 'https://batterymantra.com')
WHERE content2 LIKE '%www.batterymantra.com%';

UPDATE cms_pages
SET content2 = REPLACE(content2, 'https://batterymantra.in', 'https://batterymantra.com')
WHERE content2 LIKE '%batterymantra.in%';

-- 3. products table
UPDATE products
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.com%';

UPDATE products
SET seo_canonical_url = REPLACE(seo_canonical_url, 'http://www.batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.com%';

UPDATE products
SET seo_canonical_url = REPLACE(seo_canonical_url, 'http://batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE 'http://batterymantra.com%';

UPDATE products
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://batterymantra.in', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%batterymantra.in%';

UPDATE products
SET product_description = REPLACE(product_description, 'https://www.batterymantra.com', 'https://batterymantra.com')
WHERE product_description LIKE '%www.batterymantra.com%';

UPDATE products
SET product_description = REPLACE(product_description, 'http://www.batterymantra.com', 'https://batterymantra.com')
WHERE product_description LIKE '%www.batterymantra.com%';

UPDATE products
SET product_description = REPLACE(product_description, 'https://batterymantra.in', 'https://batterymantra.com')
WHERE product_description LIKE '%batterymantra.in%';

-- 4. categories table
UPDATE categories
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.com%';

UPDATE categories
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://batterymantra.in', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%batterymantra.in%';

-- 5. brands table
UPDATE brands
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://www.batterymantra.com', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%www.batterymantra.com%';

UPDATE brands
SET seo_canonical_url = REPLACE(seo_canonical_url, 'https://batterymantra.in', 'https://batterymantra.com')
WHERE seo_canonical_url LIKE '%batterymantra.in%';

