-- Enterprise AI Report Platform Schema

CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `contact_name` VARCHAR(50),
    `contact_email` VARCHAR(100),
    `contact_phone` VARCHAR(20),
    `status` INT DEFAULT 1,
    `max_users` INT DEFAULT 100,
    `max_datasources` INT DEFAULT 10,
    `config` JSON,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(200) NOT NULL,
    `nickname` VARCHAR(50),
    `email` VARCHAR(100),
    `phone` VARCHAR(20),
    `avatar` VARCHAR(500),
    `status` INT DEFAULT 1,
    `last_login_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `code` VARCHAR(50) NOT NULL,
    `description` VARCHAR(200),
    `status` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `name` VARCHAR(50) NOT NULL,
    `code` VARCHAR(100) NOT NULL UNIQUE,
    `type` VARCHAR(20),
    `path` VARCHAR(200),
    `icon` VARCHAR(50),
    `sort` INT DEFAULT 0,
    `status` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    `status` INT DEFAULT 1,
    `last_test_at` DATETIME,
    `last_test_result` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dataset` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `data_source_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `table_name` VARCHAR(100) NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dataset_column` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `dataset_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `display_name` VARCHAR(100),
    `data_type` VARCHAR(50),
    `length` INT,
    `precision` INT,
    `scale` INT,
    `nullable` INT DEFAULT 1,
    `description` VARCHAR(500),
    `sort` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_dataset_id` (`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `schema_definition` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `dataset_id` BIGINT NOT NULL,
    `columns` JSON,
    `metrics` JSON,
    `dimensions` JSON,
    `config` JSON,
    `version` INT DEFAULT 1,
    `status` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `kpi_definition` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `schema_id` BIGINT,
    `dataset_id` BIGINT NOT NULL,
    `expression` TEXT NOT NULL,
    `unit` VARCHAR(20),
    `aggregation_type` VARCHAR(20),
    `filter_condition` TEXT,
    `group_by` VARCHAR(200),
    `config` JSON,
    `version` INT DEFAULT 1,
    `status` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_schema_id` (`schema_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    `status` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `workflow_run` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `workflow_id` BIGINT NOT NULL,
    `trigger_type` VARCHAR(20),
    `state` VARCHAR(20) DEFAULT 'PENDING',
    `input_params` JSON,
    `output_result` JSON,
    `error_message` TEXT,
    `started_at` DATETIME,
    `finished_at` DATETIME,
    `duration_ms` BIGINT,
    `retry_count` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_workflow_id` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `workflow_node_run` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `workflow_run_id` BIGINT NOT NULL,
    `node_id` VARCHAR(50) NOT NULL,
    `node_name` VARCHAR(100),
    `node_type` VARCHAR(50),
    `state` VARCHAR(20) DEFAULT 'PENDING',
    `input_params` JSON,
    `output_result` JSON,
    `error_message` TEXT,
    `started_at` DATETIME,
    `finished_at` DATETIME,
    `duration_ms` BIGINT,
    `retry_count` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_workflow_run_id` (`workflow_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `workflow_execution_log` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `workflow_run_id` BIGINT NOT NULL,
    `node_id` VARCHAR(50),
    `level` VARCHAR(10) DEFAULT 'INFO',
    `message` TEXT,
    `details` JSON,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_workflow_run_id` (`workflow_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `workflow_state_snapshot` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `workflow_run_id` BIGINT NOT NULL,
    `snapshot_data` JSON,
    `current_node_id` VARCHAR(50),
    `completed_nodes` TEXT,
    `failed_nodes` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_workflow_run_id` (`workflow_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `config_snapshot` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `snapshot_data` JSON NOT NULL,
    `version` INT DEFAULT 1,
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    `status` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    `status` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_execution_trace` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `user_id` BIGINT,
    `operation_type` VARCHAR(50),
    `input_prompt` TEXT,
    `generated_sql` TEXT,
    `ai_model` VARCHAR(50),
    `ai_response` TEXT,
    `token_count` INT,
    `duration_ms` BIGINT,
    `status` INT DEFAULT 0,
    `error_message` TEXT,
    `metadata` JSON,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_sql_validation_log` (
    `id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    `trace_id` BIGINT,
    `original_sql` TEXT,
    `validated_sql` TEXT,
    `validation_result` JSON,
    `security_check` JSON,
    `status` INT DEFAULT 0,
    `error_message` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
