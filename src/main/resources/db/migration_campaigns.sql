-- ====================================================================
-- Database Migration: Dynamic Offers & Campaign Engine
-- ====================================================================

CREATE TABLE IF NOT EXISTS campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    tagline VARCHAR(500),
    description TEXT,
    badge_text VARCHAR(100),
    badge_theme VARCHAR(50) DEFAULT 'BRAND_RED', -- 'FESTIVE_GOLD', 'BRAND_RED', 'ROYAL_BLUE', 'EMERALD_GREEN'
    desktop_banner_url VARCHAR(1000),
    mobile_banner_url VARCHAR(1000),
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    priority INT DEFAULT 0,
    show_countdown BOOLEAN DEFAULT true,
    show_in_announcement BOOLEAN DEFAULT true,
    show_on_home BOOLEAN DEFAULT true,
    target_type VARCHAR(50) NOT NULL, -- 'ALL_PRODUCTS', 'CATEGORIES', 'BRANDS', 'CUSTOM_PRODUCTS'
    discount_type VARCHAR(50) NOT NULL, -- 'FLAT_AMOUNT', 'PERCENTAGE', 'EXTRA_EXCHANGE_BONUS', 'SHOWCASE_ONLY'
    discount_value NUMERIC(10, 2) DEFAULT 0.00,
    max_discount_cap NUMERIC(10, 2), -- Cap for percentage discount
    min_order_value NUMERIC(10, 2) DEFAULT 0.00,
    coupon_code VARCHAR(50), -- If NULL or empty, auto-applied to eligible products
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_campaigns_active ON campaigns(is_active, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_campaigns_slug ON campaigns(slug);

CREATE TABLE IF NOT EXISTS campaign_categories (
    campaign_id UUID REFERENCES campaigns(id) ON DELETE CASCADE,
    category_id UUID NOT NULL,
    PRIMARY KEY (campaign_id, category_id)
);

CREATE TABLE IF NOT EXISTS campaign_brands (
    campaign_id UUID REFERENCES campaigns(id) ON DELETE CASCADE,
    brand_id UUID NOT NULL,
    PRIMARY KEY (campaign_id, brand_id)
);

CREATE TABLE IF NOT EXISTS campaign_products (
    campaign_id UUID REFERENCES campaigns(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    PRIMARY KEY (campaign_id, product_id)
);

CREATE TABLE IF NOT EXISTS campaign_excluded_products (
    campaign_id UUID REFERENCES campaigns(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    PRIMARY KEY (campaign_id, product_id)
);

-- Update orders table so financial records remain immutable when an offer is claimed
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS applied_campaign_id UUID,
ADD COLUMN IF NOT EXISTS campaign_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS campaign_discount_amount NUMERIC(10, 2) DEFAULT 0.00;
