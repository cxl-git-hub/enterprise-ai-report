-- Migration: Add data lineage tracking and scheduled reports tables
-- Run this against your database to add the new features

-- ============================================
-- 1. Data Lineage Table
-- Tracks which data sources were used in AI outputs
-- ============================================
CREATE TABLE IF NOT EXISTS data_lineage (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    ref_type VARCHAR(50) NOT NULL COMMENT 'Reference type: report_output, analysis, nl2sql, suggestion',
    ref_id VARCHAR(100) NOT NULL COMMENT 'Reference ID (e.g., report output ID, trace ID)',
    dataset_id BIGINT COMMENT 'Source dataset ID',
    schema_id BIGINT COMMENT 'Source schema ID',
    source_name VARCHAR(255) COMMENT 'Source name (human-readable)',
    fields TEXT COMMENT 'Comma-separated field names used',
    time_range VARCHAR(100) COMMENT 'Time range of data used',
    row_count BIGINT COMMENT 'Number of rows involved',
    quality VARCHAR(20) COMMENT 'Data quality assessment: high/medium/low',
    generated_sql TEXT COMMENT 'SQL generated from this source',
    confidence_score INT COMMENT 'Confidence score (0-100)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_ref (tenant_id, ref_type, ref_id),
    INDEX idx_dataset (dataset_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 2. Report Schedule Table
-- Scheduled report generation and distribution
-- ============================================
CREATE TABLE IF NOT EXISTS report_schedule (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    report_template_id BIGINT COMMENT 'Associated report template ID',
    format VARCHAR(20) DEFAULT 'pdf' COMMENT 'Output format: pdf/docx/excel/pptx/html',
    cron_expression VARCHAR(100) COMMENT 'Cron expression for scheduling',
    timezone VARCHAR(50) DEFAULT 'Asia/Shanghai',
    recipients TEXT COMMENT 'Comma-separated recipient email addresses',
    cc_recipients TEXT COMMENT 'CC email addresses',
    email_subject VARCHAR(500) COMMENT 'Email subject template',
    email_body TEXT COMMENT 'Email body template',
    include_disclaimer TINYINT(1) DEFAULT 1 COMMENT 'Whether to include AI disclaimer',
    include_lineage TINYINT(1) DEFAULT 1 COMMENT 'Whether to include data lineage',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'Schedule status: active/paused/error',
    last_run_at DATETIME COMMENT 'Last execution time',
    next_run_at DATETIME COMMENT 'Next scheduled execution time',
    last_error TEXT COMMENT 'Last error message',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status),
    INDEX idx_next_run (next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 3. Report Comments Table
-- Collaboration: comments and annotations on reports
-- ============================================
CREATE TABLE IF NOT EXISTS report_comment (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    ref_type VARCHAR(50) NOT NULL COMMENT 'Reference type: report_output, report_template, analysis',
    ref_id VARCHAR(100) NOT NULL COMMENT 'Reference ID',
    user_id BIGINT NOT NULL COMMENT 'Comment author',
    parent_id BIGINT COMMENT 'Parent comment ID for replies',
    mention_user_id BIGINT COMMENT 'Mentioned user ID',
    content TEXT NOT NULL COMMENT 'Comment content',
    likes INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    INDEX idx_tenant_ref (tenant_id, ref_type, ref_id),
    INDEX idx_user (user_id),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 4. Report Bookmarks Table
-- User bookmarks/favorites for reports
-- ============================================
CREATE TABLE IF NOT EXISTS report_bookmark (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    ref_type VARCHAR(50) NOT NULL,
    ref_id VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_ref (user_id, ref_type, ref_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5. Share Links Table
-- Shareable links for reports
-- ============================================
CREATE TABLE IF NOT EXISTS share_link (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    ref_type VARCHAR(50) NOT NULL,
    ref_id VARCHAR(100) NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE COMMENT 'Share token',
    include_disclaimer TINYINT(1) DEFAULT 1,
    include_lineage TINYINT(1) DEFAULT 1,
    expires_at DATETIME COMMENT 'Expiration time',
    access_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
