-- ==============================================================================
-- Migration: Centralized Stock & Inventory Management System
-- ==============================================================================

-- 1. Ensure columns exist on products table
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS product_stock INT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS low_stock_threshold INT NOT NULL DEFAULT 10,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- 2. Non-negative stock constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_product_stock_non_negative'
    ) THEN
        ALTER TABLE products ADD CONSTRAINT chk_product_stock_non_negative CHECK (product_stock >= 0);
    END IF;
END $$;

-- 3. Index for instant inventory filtering
CREATE INDEX IF NOT EXISTS idx_products_stock ON products(product_stock);

-- 4. Ensure stock_restored column exists on orders table
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS stock_restored BOOLEAN NOT NULL DEFAULT false;

-- 5. Stock audit logs table
CREATE TABLE IF NOT EXISTS stock_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    change_type VARCHAR(50) NOT NULL, -- 'ORDER_DEDUCT', 'ORDER_RESTORE', 'MANUAL_ADJUSTMENT', 'BULK_UPDATE'
    quantity_change INT NOT NULL,     -- e.g. -2 for order, +2 for cancel, +20 for restock
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    order_id UUID REFERENCES orders(order_id) ON DELETE SET NULL,
    performed_by VARCHAR(255),        -- 'SYSTEM' or Admin user email
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stock_audit_product ON stock_audit_logs(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_audit_order ON stock_audit_logs(order_id);

