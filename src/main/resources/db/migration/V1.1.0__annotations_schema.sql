-- ==============================================================================
-- FIELD ENGINEERING POSTGRESQL SCHEMA UPDATE (V1.1.0)
-- Enhanced Drawing Annotations, Layers & Comments
-- ==============================================================================

-- 1. Add missing columns to markups table if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='markups' AND column_name='status') THEN
        ALTER TABLE markups ADD COLUMN status VARCHAR(64) NOT NULL DEFAULT 'Open';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='markups' AND column_name='deleted') THEN
        ALTER TABLE markups ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='markups' AND column_name='revision_id') THEN
        ALTER TABLE markups ADD COLUMN revision_id VARCHAR(64);
    END IF;
END $$;

-- 2. Markup Layers Table
CREATE TABLE IF NOT EXISTS markup_layers (
    id VARCHAR(64) PRIMARY KEY,
    revision_id VARCHAR(64),
    drawing_id VARCHAR(64),
    name VARCHAR(255) NOT NULL DEFAULT 'Default Markup',
    category VARCHAR(64) NOT NULL DEFAULT 'Field Note',
    owner_id VARCHAR(64),
    visible BOOLEAN NOT NULL DEFAULT true,
    locked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Annotation Comments Table
CREATE TABLE IF NOT EXISTS annotation_comments (
    id VARCHAR(64) PRIMARY KEY,
    markup_id VARCHAR(64) NOT NULL,
    author_id VARCHAR(64),
    author_name VARCHAR(255) NOT NULL DEFAULT 'Field Engineer',
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_markups_status ON markups(status);
CREATE INDEX IF NOT EXISTS idx_markups_revision ON markups(revision_id);
CREATE INDEX IF NOT EXISTS idx_annotation_comments_markup ON annotation_comments(markup_id);
CREATE INDEX IF NOT EXISTS idx_markup_layers_revision ON markup_layers(revision_id);
