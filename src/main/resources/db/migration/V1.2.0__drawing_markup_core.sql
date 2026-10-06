-- Migration V1.2.0: Core Drawing Markup Tables

CREATE TABLE IF NOT EXISTS drawing_files (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    file_key VARCHAR(512) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    page_count INT NOT NULL DEFAULT 1,
    file_size BIGINT NOT NULL DEFAULT 0,
    owner_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS page_markups (
    id VARCHAR(64) PRIMARY KEY,
    drawing_id VARCHAR(64) NOT NULL,
    page_number INT NOT NULL DEFAULT 1,
    payload TEXT,
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_page_markups_drawing ON page_markups(drawing_id, page_number);
