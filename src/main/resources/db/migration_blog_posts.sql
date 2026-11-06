-- ============================================================================
-- Battery Mantra - Blog Management System Migration
-- Table: blog_posts
-- ============================================================================

CREATE TABLE IF NOT EXISTS blog_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    excerpt TEXT,
    content TEXT NOT NULL,
    cover_image VARCHAR(512),
    category VARCHAR(100) NOT NULL,
    tags VARCHAR(255),
    author_name VARCHAR(100) DEFAULT 'Battery Mantra Team',
    author_image VARCHAR(512),
    read_time_minutes INT DEFAULT 4,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_featured BOOLEAN DEFAULT FALSE,
    view_count BIGINT DEFAULT 0,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- SEO Metadata
    meta_title VARCHAR(255),
    meta_description TEXT,
    meta_keywords VARCHAR(512),
    canonical_url VARCHAR(512),
    og_image VARCHAR(512),

    -- E-Commerce Conversion: Store array of related Product UUIDs
    related_product_ids JSONB DEFAULT '[]'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_blog_posts_slug ON blog_posts(slug);
CREATE INDEX IF NOT EXISTS idx_blog_posts_status ON blog_posts(status);
CREATE INDEX IF NOT EXISTS idx_blog_posts_category ON blog_posts(category);
CREATE INDEX IF NOT EXISTS idx_blog_posts_published_at ON blog_posts(published_at DESC);

