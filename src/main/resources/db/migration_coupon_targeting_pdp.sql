-- ====================================================================
-- Database Migration: Granular Coupon Targeting & Public PDP Display
-- ====================================================================

-- 1. Safely handle table rename if the table currently exists as 'coupon' (singular)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'coupon')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'coupons') THEN
        ALTER TABLE coupon RENAME TO coupons;
    END IF;

    -- If primary key column was 'coupon_id', rename to 'id' if 'id' does not already exist
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'coupons' AND column_name = 'coupon_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'coupons' AND column_name = 'id'
    ) THEN
        ALTER TABLE coupons RENAME COLUMN coupon_id TO id;
    END IF;
END $$;

-- 2. Create coupons table if it does not exist at all
CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(10, 2) NOT NULL,
    max_discount_amount NUMERIC(10, 2),
    min_order_value NUMERIC(10, 2) DEFAULT 0.0,
    usage_limit INT,
    used_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    start_date TIMESTAMP,
    expiry_date TIMESTAMP,
    target_type VARCHAR(50) NOT NULL DEFAULT 'ALL_PRODUCTS',
    target_category_ids JSONB DEFAULT '[]'::jsonb,
    target_category_names JSONB DEFAULT '[]'::jsonb,
    target_brand_ids JSONB DEFAULT '[]'::jsonb,
    target_brand_names JSONB DEFAULT '[]'::jsonb,
    target_product_ids JSONB DEFAULT '[]'::jsonb,
    excluded_product_ids JSONB DEFAULT '[]'::jsonb,
    display_on_pdp BOOLEAN NOT NULL DEFAULT true,
    pdp_highlight_text VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 3. Add targeting and PDP display columns to coupons table if upgrading existing schema
ALTER TABLE coupons
ADD COLUMN IF NOT EXISTS target_type VARCHAR(50) NOT NULL DEFAULT 'ALL_PRODUCTS',
ADD COLUMN IF NOT EXISTS target_category_ids JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS target_category_names JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS target_brand_ids JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS target_brand_names JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS target_product_ids JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS excluded_product_ids JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS display_on_pdp BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS pdp_highlight_text VARCHAR(255) DEFAULT NULL;

-- 4. Index for instant lookup of active public coupons for PDP & storefront
CREATE INDEX IF NOT EXISTS idx_coupons_active_pdp 
ON coupons(is_active, display_on_pdp, expiry_date);

