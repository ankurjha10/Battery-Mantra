-- ====================================================================
-- Database Migration: Add slug column to vehicle and brand tables
-- ====================================================================

-- 1. VEHICLE SLUG MIGRATION
-- Add slug column to vehicle table
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_vehicle_slug ON vehicle(slug);

-- Populate existing vehicles with default slug using manufacturer and fuel
UPDATE vehicle v
SET slug = LOWER(TRIM(REGEXP_REPLACE(
    CONCAT(COALESCE(m.name, ''), '-', v.model, CASE WHEN f.fuel_name IS NOT NULL AND f.fuel_name != '' THEN CONCAT('-', f.fuel_name) ELSE '' END),
    '[^a-zA-Z0-9]+', '-', 'g'
), '-'))
FROM manufacturer m
LEFT JOIN fuels f ON v.fuel_id = f.fuel_id
WHERE v.manufacturer_id = m.id
  AND (v.slug IS NULL OR v.slug = '');

-- If an alternative/alias table named 'vehicles' is used:
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'vehicles') THEN
        ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
        CREATE INDEX IF NOT EXISTS idx_vehicles_slug ON vehicles(slug);
        
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'vehicles' AND column_name = 'make') THEN
            UPDATE vehicles 
            SET slug = LOWER(TRIM(REGEXP_REPLACE(CONCAT(make, '-', model, '-', COALESCE(fuel_name, '')), '[^a-zA-Z0-9]+', '-', 'g'), '-'))
            WHERE slug IS NULL OR slug = '';
        END IF;
    END IF;
END $$;

-- 2. BRAND SLUG MIGRATION
-- Add dedicated slug column to brands table
ALTER TABLE brands ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_brands_slug ON brands(slug);

-- Populate slug from brand_name if empty
UPDATE brands
SET slug = LOWER(TRIM(REGEXP_REPLACE(brand_name, '[^a-zA-Z0-9]+', '-', 'g'), '-'))
WHERE slug IS NULL OR slug = '';

-- Also ensure seo_slug (embedded SeoMetadata) is populated if empty
UPDATE brands
SET seo_slug = LOWER(TRIM(REGEXP_REPLACE(brand_name, '[^a-zA-Z0-9]+', '-', 'g'), '-'))
WHERE seo_slug IS NULL OR seo_slug = '';
