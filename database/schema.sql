-- ============================================================
-- Enterprise AI Automated Reporting Platform - Database Schema
-- Aligned with Java Entity definitions
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ai_report` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_report`;

-- ============================================================
-- 1. AUTH + RBAC + TENANT
-- ============================================================

CREATE TABLE IF NOT EXISTS `tenant` (
  `id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `contact_name` VARCHAR(50),
  `contact_email` VARCHAR(100),
  `contact_phone` VARCHAR(20),
  `plan_type` VARCHAR(20) DEFAULT 'standard',
  `status` INT DEFAULT 1,
  `max_users` INT DEFAULT 100,
  `max_datasources` INT DEFAULT 10,
  `max_datasets` INT DEFAULT 50,
  `max_ai_calls_per_day` INT DEFAULT 1000,
  `expire_time` DATETIME,
  `config` JSON,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(200) NOT NULL,
  `real_name` VARCHAR(50),
  `email` VARCHAR(100),
  `phone` VARCHAR(20),
  `avatar` VARCHAR(500),
  `status` INT DEFAULT 1,
  `last_login_time` DATETIME,
  `last_login_ip` VARCHAR(50),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `role_code` VARCHAR(50) NOT NULL,
  `role_name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(200),
  `is_system` INT DEFAULT 0,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role` (`tenant_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` BIGINT NOT NULL,
  `parent_id` BIGINT DEFAULT 0,
  `perm_code` VARCHAR(100) NOT NULL UNIQUE,
  `perm_name` VARCHAR(100) NOT NULL,
  `perm_type` VARCHAR(20),
  `path` VARCHAR(200),
  `icon` VARCHAR(50),
  `sort_order` INT DEFAULT 0,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================================
-- 2. DATA HUB
-- ============================================================

CREATE TABLE IF NOT EXISTS `data_source` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `host` VARCHAR(200),
  `port` INT,
  `database_name` VARCHAR(100),
  `username` VARCHAR(100),
  `encrypted_password` VARCHAR(500),
  `connection_url` VARCHAR(500),
  `config` JSON,
  `description` VARCHAR(500),
  `status` INT DEFAULT 1,
  `last_test_at` DATETIME,
  `last_test_result` VARCHAR(500),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

CREATE TABLE IF NOT EXISTS `dataset` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `data_source_id` BIGINT,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500),
  `table_name` VARCHAR(100),
  `query_sql` TEXT,
  `config` JSON,
  `status` INT DEFAULT 1,
  `last_sync_at` DATETIME,
  `row_count` BIGINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_data_source_id` (`data_source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集表';

CREATE TABLE IF NOT EXISTS `dataset_column` (
  `id` BIGINT NOT NULL,
  `dataset_id` BIGINT NOT NULL,
  `column_name` VARCHAR(100) NOT NULL,
  `column_type` VARCHAR(50),
  `display_name` VARCHAR(100),
  `is_primary_key` INT DEFAULT 0,
  `is_nullable` INT DEFAULT 1,
  `description` VARCHAR(500),
  `sample_values` JSON,
  `sort_order` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dataset_id` (`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集列定义';

-- ============================================================
-- 3. SCHEMA REGISTRY
-- ============================================================

CREATE TABLE IF NOT EXISTS `schema_definition` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500),
  `dataset_id` BIGINT,
  `columns` JSON,
  `metrics` JSON,
  `dimensions` JSON,
  `config` JSON,
  `version` INT DEFAULT 1,
  `status` VARCHAR(16) DEFAULT 'active',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Schema定义表';

-- ============================================================
-- 4. KPI ENGINE
-- ============================================================

CREATE TABLE IF NOT EXISTS `kpi_definition` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500),
  `schema_id` BIGINT,
  `dataset_id` BIGINT,
  `expression` TEXT NOT NULL,
  `unit` VARCHAR(20),
  `aggregation_type` VARCHAR(20),
  `filter_condition` TEXT,
  `group_by` VARCHAR(200),
  `config` JSON,
  `version` INT DEFAULT 1,
  `status` VARCHAR(16) DEFAULT 'active',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_schema_id` (`schema_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KPI定义表';

CREATE TABLE IF NOT EXISTS `kpi_result` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `kpi_id` BIGINT NOT NULL,
  `workflow_run_id` BIGINT,
  `value` DECIMAL(20,6),
  `formatted_value` VARCHAR(100),
  `period_start` VARCHAR(50),
  `period_end` VARCHAR(50),
  `dimensions` JSON,
  `metadata` JSON,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_kpi_id` (`kpi_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KPI计算结果表';

-- ============================================================
-- 5. WORKFLOW ENGINE
-- ============================================================

CREATE TABLE IF NOT EXISTS `workflow_definition` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500),
  `dag_definition` JSON NOT NULL,
  `trigger_type` VARCHAR(20) DEFAULT 'manual',
  `cron_expression` VARCHAR(50),
  `config` JSON,
  `version` INT DEFAULT 1,
  `state` VARCHAR(20) DEFAULT 'PENDING',
  `status` VARCHAR(16) DEFAULT 'active',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义表';

-- ============================================================
-- 6. EXECUTION STATE & OBSERVABILITY
-- ============================================================

CREATE TABLE IF NOT EXISTS `workflow_run` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `workflow_id` BIGINT NOT NULL,
  `workflow_version` INT DEFAULT 1,
  `run_id` VARCHAR(64) NOT NULL,
  `trigger_type` VARCHAR(20),
  `triggered_by` BIGINT,
  `state` VARCHAR(20) DEFAULT 'PENDING',
  `current_node_id` VARCHAR(128),
  `input_params` JSON,
  `output_result` JSON,
  `error_message` TEXT,
  `start_time` DATETIME,
  `end_time` DATETIME,
  `duration_ms` BIGINT,
  `total_tokens` BIGINT DEFAULT 0,
  `total_cost` DECIMAL(10,4) DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_run_id` (`run_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流运行记录表';

CREATE TABLE IF NOT EXISTS `workflow_node_run` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT,
  `run_id` VARCHAR(64) NOT NULL,
  `node_id` VARCHAR(128) NOT NULL,
  `node_type` VARCHAR(50),
  `node_name` VARCHAR(100),
  `state` VARCHAR(20) DEFAULT 'PENDING',
  `input_data` JSON,
  `output_data` JSON,
  `error_message` TEXT,
  `retry_count` INT DEFAULT 0,
  `start_time` DATETIME,
  `end_time` DATETIME,
  `duration_ms` BIGINT,
  `tokens_used` BIGINT DEFAULT 0,
  `cost` DECIMAL(10,4) DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_run_id` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点执行记录表';

CREATE TABLE IF NOT EXISTS `workflow_execution_log` (
  `id` BIGINT NOT NULL,
  `run_id` VARCHAR(64) NOT NULL,
  `node_id` VARCHAR(128),
  `log_level` VARCHAR(10) DEFAULT 'INFO',
  `message` TEXT,
  `context_data` JSON,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_run_id` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流执行日志表';

CREATE TABLE IF NOT EXISTS `workflow_state_snapshot` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT,
  `workflow_run_id` BIGINT NOT NULL,
  `snapshot_data` JSON,
  `current_node_id` VARCHAR(128),
  `completed_nodes` TEXT,
  `failed_nodes` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_workflow_run_id` (`workflow_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流状态快照表';

-- ============================================================
-- 7. CONFIG CONSISTENCY ENGINE
-- ============================================================

CREATE TABLE IF NOT EXISTS `config_version` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `config_type` VARCHAR(20) NOT NULL,
  `config_id` BIGINT NOT NULL,
  `version` INT NOT NULL,
  `config_data` JSON NOT NULL,
  `change_description` VARCHAR(500),
  `created_by` BIGINT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_config` (`config_type`, `config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置版本表';

CREATE TABLE IF NOT EXISTS `config_dependency_graph` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `source_type` VARCHAR(20) NOT NULL,
  `source_id` BIGINT NOT NULL,
  `source_name` VARCHAR(100),
  `target_type` VARCHAR(20) NOT NULL,
  `target_id` BIGINT NOT NULL,
  `target_name` VARCHAR(100),
  `dependency_type` VARCHAR(20),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_source` (`tenant_id`, `source_type`, `source_id`),
  KEY `idx_tenant_target` (`tenant_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置依赖关系图';

CREATE TABLE IF NOT EXISTS `config_snapshot` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `snapshot_name` VARCHAR(100) NOT NULL,
  `snapshot_version` VARCHAR(64) NOT NULL,
  `description` VARCHAR(500),
  `full_snapshot` JSON NOT NULL,
  `created_by` BIGINT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_version` (`tenant_id`, `snapshot_version`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置快照表';

-- ============================================================
-- 8. PROMPT & REPORT TEMPLATES
-- ============================================================

CREATE TABLE IF NOT EXISTS `prompt_template` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500),
  `schema_id` BIGINT,
  `template_content` TEXT NOT NULL,
  `variables` JSON,
  `config` JSON,
  `version` INT DEFAULT 1,
  `status` VARCHAR(16) DEFAULT 'active',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt模板表';

CREATE TABLE IF NOT EXISTS `report_template` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500),
  `format` VARCHAR(10) NOT NULL,
  `template_file` VARCHAR(500),
  `variables` JSON,
  `config` JSON,
  `version` INT DEFAULT 1,
  `status` VARCHAR(16) DEFAULT 'active',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表模板表';

-- ============================================================
-- 9. AI CONTROL & SAFETY ENGINE
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_policy` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500),
  `allow_sql_generation` TINYINT DEFAULT 1,
  `allow_cross_dataset_join` TINYINT DEFAULT 0,
  `allow_data_modification` TINYINT DEFAULT 0,
  `max_rows_returned` INT DEFAULT 1000,
  `max_execution_time` INT DEFAULT 30,
  `allowed_datasets` JSON,
  `blocked_tables` JSON,
  `config` JSON,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI策略表';

CREATE TABLE IF NOT EXISTS `ai_execution_trace` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `trace_id` VARCHAR(64) NOT NULL,
  `run_id` VARCHAR(64),
  `node_id` VARCHAR(128),
  `ai_task_type` VARCHAR(32),
  `input_prompt` TEXT,
  `prompt_tokens` INT DEFAULT 0,
  `completion_tokens` INT DEFAULT 0,
  `total_tokens` INT DEFAULT 0,
  `model_name` VARCHAR(64),
  `model_config` JSON,
  `raw_output` TEXT,
  `validated_output` TEXT,
  `validation_passed` TINYINT,
  `validation_errors` JSON,
  `retry_count` INT DEFAULT 0,
  `latency_ms` BIGINT,
  `cost` DECIMAL(10,6) DEFAULT 0,
  `status` VARCHAR(16) DEFAULT 'pending',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trace_id` (`trace_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI执行追踪表';

CREATE TABLE IF NOT EXISTS `ai_sql_validation_log` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT,
  `trace_id` VARCHAR(64),
  `original_sql` TEXT,
  `validated_sql` TEXT,
  `validation_result` JSON,
  `security_check` JSON,
  `status` INT DEFAULT 0,
  `error_message` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI SQL校验日志表';

-- ============================================================
-- 10. OUTPUT ENGINE
-- ============================================================

CREATE TABLE IF NOT EXISTS `report_output` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `workflow_run_id` BIGINT,
  `report_template_id` BIGINT,
  `name` VARCHAR(200) NOT NULL,
  `format` VARCHAR(10) NOT NULL,
  `file_key` VARCHAR(500),
  `file_name` VARCHAR(200),
  `file_size` BIGINT,
  `file_path` VARCHAR(500),
  `status` INT DEFAULT 1,
  `error_message` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_workflow_run_id` (`workflow_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表输出表';

-- ============================================================
-- 11. NOTIFICATIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(16) NOT NULL,
  `title` VARCHAR(256),
  `message` TEXT NOT NULL,
  `link` VARCHAR(512),
  `is_read` TINYINT DEFAULT 0,
  `source_type` VARCHAR(32),
  `source_id` VARCHAR(64),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ============================================================
-- 12. SYSTEM SETTINGS
-- ============================================================

CREATE TABLE IF NOT EXISTS `system_setting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `setting_group` VARCHAR(32) NOT NULL,
  `setting_key` VARCHAR(128) NOT NULL,
  `setting_value` JSON,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_group_key` (`tenant_id`, `setting_group`, `setting_key`),
  KEY `idx_tenant_group` (`tenant_id`, `setting_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

-- ============================================================
-- 13. AUDIT LOG
-- ============================================================

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `user_id` BIGINT,
  `username` VARCHAR(50),
  `action` VARCHAR(50) NOT NULL,
  `resource_type` VARCHAR(50),
  `resource_id` BIGINT,
  `resource_name` VARCHAR(200),
  `details` TEXT,
  `ip_address` VARCHAR(50),
  `user_agent` VARCHAR(500),
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ============================================================
-- 14. DEMO DATA
-- ============================================================

-- Default tenant
INSERT INTO `tenant` (`id`, `name`, `code`, `contact_name`, `contact_email`, `status`, `max_users`, `max_datasources`)
VALUES (1, '默认租户', 'default', 'Admin', 'admin@enterprise.com', 1, 100, 500);

-- Default roles
INSERT INTO `sys_role` (`id`, `tenant_id`, `role_code`, `role_name`, `description`, `is_system`, `status`) VALUES
(1, 1, 'SUPER_ADMIN', '超级管理员', '系统超级管理员', 1, 1),
(2, 1, 'TENANT_ADMIN', '租户管理员', '租户管理员', 1, 1),
(3, 1, 'DATA_ANALYST', '数据分析师', '数据分析师', 0, 1),
(4, 1, 'REPORT_VIEWER', '报表查看者', '只读用户', 0, 1);

-- Default admin user (password: admin123 BCrypt encoded)
INSERT INTO `sys_user` (`id`, `tenant_id`, `username`, `password`, `real_name`, `email`, `status`) VALUES
(1, 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@enterprise.com', 1);

INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES (1, 1, 1);

-- Default permissions
INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_code`, `perm_name`, `perm_type`, `sort_order`, `status`) VALUES
(1, 0, 'user:manage', '用户管理', 'menu', 1, 1),
(2, 0, 'config:manage', '配置管理', 'menu', 2, 1),
(3, 0, 'workflow:execute', '工作流执行', 'menu', 3, 1),
(4, 0, 'data:manage', '数据管理', 'menu', 4, 1),
(5, 0, 'ai:use', 'AI功能使用', 'menu', 5, 1),
(6, 0, 'report:view', '报表查看', 'menu', 6, 1),
(7, 0, 'audit:view', '审计查看', 'menu', 7, 1);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6), (7, 1, 7);

-- Default AI policy
INSERT INTO `ai_policy` (`id`, `tenant_id`, `name`, `description`, `allow_sql_generation`, `allow_cross_dataset_join`, `allow_data_modification`, `max_rows_returned`, `max_execution_time`, `status`) VALUES
(1, 1, '默认AI策略', '默认租户AI策略', 1, 0, 0, 10000, 60, 1);
