-- ==============================================================================
-- Migration: Deferred Online Order Creation (Pending Payment Checkouts)
-- Orders are only created in `orders` after payment is verified or captured.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS pending_payment_checkouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(255),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    address_id UUID NOT NULL REFERENCES address(address_id),
    delivery_method VARCHAR(100) NOT NULL,
    installation_date DATE,
    total_amount NUMERIC(38, 2) NOT NULL,
    exchange_discount NUMERIC(38, 2),
    selected_city VARCHAR(255),
    applied_campaign_id UUID,
    campaign_name VARCHAR(255),
    campaign_discount_amount NUMERIC(38, 2) DEFAULT 0,
    discount_amount NUMERIC(38, 2),
    coupon_code VARCHAR(255),
    assigned_partner_id UUID REFERENCES partner_profiles(id),
    items_snapshot_json TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(500),
    created_order_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pending_checkout_rzp_order
    ON pending_payment_checkouts(razorpay_order_id);

CREATE INDEX IF NOT EXISTS idx_pending_checkout_status_expires
    ON pending_payment_checkouts(status, expires_at);
