-- ====================================================================
-- Database Migration: Migrate Cloudinary Image URLs to Local VPS Disk Storage
-- ====================================================================

-- 1. Products table
UPDATE products 
SET product_image = REGEXP_REPLACE(
    product_image, 
    '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?', 
    'https://batterymantra.com/uploads/'
)
WHERE product_image LIKE '%res.cloudinary.com%';

-- 2. Categories table
UPDATE categories 
SET icon_url = REGEXP_REPLACE(
    icon_url, 
    '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?', 
    'https://batterymantra.com/uploads/'
)
WHERE icon_url LIKE '%res.cloudinary.com%';

-- 3. Brands table
UPDATE brands 
SET brand_logo = REGEXP_REPLACE(
    brand_logo, 
    '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?', 
    'https://batterymantra.com/uploads/'
)
WHERE brand_logo LIKE '%res.cloudinary.com%';

-- 4. Banners table
UPDATE banners 
SET image_url = REGEXP_REPLACE(
    image_url, 
    '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?', 
    'https://batterymantra.com/uploads/'
)
WHERE image_url LIKE '%res.cloudinary.com%';

-- 5. Vehicles table
UPDATE vehicles 
SET image_url = REGEXP_REPLACE(
    image_url, 
    '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?', 
    'https://batterymantra.com/uploads/'
)
WHERE image_url LIKE '%res.cloudinary.com%';

-- 6. Additional image columns (Categories banner_url, Campaigns, Blog Posts)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'banner_url') THEN
        UPDATE categories
        SET banner_url = REGEXP_REPLACE(
            banner_url,
            '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?',
            'https://batterymantra.com/uploads/'
        )
        WHERE banner_url LIKE '%res.cloudinary.com%';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaigns' AND column_name = 'desktop_banner_url') THEN
        UPDATE campaigns
        SET desktop_banner_url = REGEXP_REPLACE(
            desktop_banner_url,
            '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?',
            'https://batterymantra.com/uploads/'
        )
        WHERE desktop_banner_url LIKE '%res.cloudinary.com%';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaigns' AND column_name = 'mobile_banner_url') THEN
        UPDATE campaigns
        SET mobile_banner_url = REGEXP_REPLACE(
            mobile_banner_url,
            '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?',
            'https://batterymantra.com/uploads/'
        )
        WHERE mobile_banner_url LIKE '%res.cloudinary.com%';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'blog_posts' AND column_name = 'cover_image') THEN
        UPDATE blog_posts
        SET cover_image = REGEXP_REPLACE(
            cover_image,
            '^https?://res\.cloudinary\.com/[^/]+/image/upload/(v[0-9]+/)?',
            'https://batterymantra.com/uploads/'
        )
        WHERE cover_image LIKE '%res.cloudinary.com%';
    END IF;
END $$;

