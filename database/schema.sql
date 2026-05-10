-- ============================================================
-- Enterprise AI Automated Reporting Platform - Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ai_report` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_report`;

-- ============================================================
-- 1. AUTH + RBAC + TENANT
-- ============================================================

CREATE TABLE `tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
  `tenant_name` VARCHAR(128) NOT NULL COMMENT '租户名称',
  `contact_name` VARCHAR(64) DEFAULT NULL COMMENT '联系人',
  `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
  `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `plan_type` VARCHAR(32) NOT NULL DEFAULT 'standard' COMMENT '套餐类型: standard/pro/enterprise',
  `max_users` INT NOT NULL DEFAULT 10 COMMENT '最大用户数',
  `max_datasets` INT NOT NULL DEFAULT 50 COMMENT '最大数据集数',
  `max_ai_calls_per_day` INT NOT NULL DEFAULT 1000 COMMENT '每日AI调用上限',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='租户表';

CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(256) NOT NULL COMMENT '密码(BCrypt)',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `email` VARCHAR(128) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `avatar` VARCHAR(512) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
  `last_login_time` DATETIME DEFAULT NULL,
  `last_login_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(256) DEFAULT NULL,
  `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色',
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role` (`tenant_id`, `role_code`)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE `sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID',
  `perm_code` VARCHAR(128) NOT NULL COMMENT '权限编码',
  `perm_name` VARCHAR(128) NOT NULL COMMENT '权限名称',
  `perm_type` VARCHAR(16) NOT NULL COMMENT 'menu/button/api',
  `path` VARCHAR(256) DEFAULT NULL COMMENT '路由路径',
  `icon` VARCHAR(64) DEFAULT NULL,
  `sort_order` INT DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`)
) ENGINE=InnoDB COMMENT='权限表';

CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB COMMENT='用户角色关联表';

CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- ============================================================
-- 2. DATA HUB - Data Source & Dataset Management
-- ============================================================

CREATE TABLE `data_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `source_name` VARCHAR(128) NOT NULL COMMENT '数据源名称',
  `source_type` VARCHAR(32) NOT NULL COMMENT 'mysql/postgresql/api/excel/minio',
  `connection_config` JSON NOT NULL COMMENT '连接配置(加密存储)',
  `description` VARCHAR(512) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `last_sync_time` DATETIME DEFAULT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB COMMENT='数据源表';

CREATE TABLE `dataset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `source_id` BIGINT DEFAULT NULL COMMENT '关联数据源ID',
  `dataset_name` VARCHAR(128) NOT NULL COMMENT '数据集名称',
  `dataset_type` VARCHAR(32) NOT NULL COMMENT 'table/query/file/api',
  `storage_location` VARCHAR(512) DEFAULT NULL COMMENT '存储位置(MinIO路径)',
  `row_count` BIGINT DEFAULT 0,
  `column_count` INT DEFAULT 0,
  `description` VARCHAR(512) DEFAULT NULL,
  `tags` JSON DEFAULT NULL COMMENT '标签',
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_source_id` (`source_id`)
) ENGINE=InnoDB COMMENT='数据集表';

CREATE TABLE `dataset_column` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dataset_id` BIGINT NOT NULL,
  `column_name` VARCHAR(128) NOT NULL,
  `column_type` VARCHAR(32) NOT NULL COMMENT 'string/number/date/boolean',
  `display_name` VARCHAR(128) DEFAULT NULL,
  `is_primary_key` TINYINT DEFAULT 0,
  `is_nullable` TINYINT DEFAULT 1,
  `description` VARCHAR(256) DEFAULT NULL,
  `sample_values` JSON DEFAULT NULL COMMENT '示例值',
  `sort_order` INT DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dataset_id` (`dataset_id`)
) ENGINE=InnoDB COMMENT='数据集列定义';

-- ============================================================
-- 3. SCHEMA REGISTRY (Config Consistency Engine foundation)
-- ============================================================

CREATE TABLE `schema_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `schema_code` VARCHAR(128) NOT NULL COMMENT 'Schema编码',
  `schema_name` VARCHAR(128) NOT NULL COMMENT 'Schema名称',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `dataset_id` BIGINT DEFAULT NULL COMMENT '关联数据集',
  `column_definitions` JSON NOT NULL COMMENT '列定义(JSON Schema格式)',
  `validation_rules` JSON DEFAULT NULL COMMENT '校验规则',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'draft/active/deprecated',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schema_version` (`tenant_id`, `schema_code`, `version`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB COMMENT='Schema定义表(版本化)';

-- ============================================================
-- 4. KPI ENGINE
-- ============================================================

CREATE TABLE `kpi_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `kpi_code` VARCHAR(128) NOT NULL COMMENT 'KPI编码',
  `kpi_name` VARCHAR(128) NOT NULL COMMENT 'KPI名称',
  `version` INT NOT NULL DEFAULT 1,
  `schema_id` BIGINT NOT NULL COMMENT '关联Schema ID',
  `schema_version` INT NOT NULL COMMENT '关联Schema版本',
  `kpi_type` VARCHAR(32) NOT NULL COMMENT 'count/sum/avg/ratio/custom',
  `expression` TEXT NOT NULL COMMENT 'KPI计算表达式(DSL)',
  `dimensions` JSON DEFAULT NULL COMMENT '维度定义',
  `filters` JSON DEFAULT NULL COMMENT '过滤条件',
  `unit` VARCHAR(32) DEFAULT NULL COMMENT '单位',
  `description` VARCHAR(512) DEFAULT NULL,
  `business_explanation` TEXT DEFAULT NULL COMMENT '业务解释',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kpi_version` (`tenant_id`, `kpi_code`, `version`),
  KEY `idx_schema_id` (`schema_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB COMMENT='KPI定义表(版本化)';

CREATE TABLE `kpi_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `kpi_id` BIGINT NOT NULL COMMENT 'KPI定义ID',
  `kpi_version` INT NOT NULL,
  `execution_id` VARCHAR(64) DEFAULT NULL COMMENT '执行批次ID',
  `dimension_values` JSON DEFAULT NULL COMMENT '维度值',
  `result_value` DECIMAL(20,4) DEFAULT NULL COMMENT '计算结果',
  `result_text` VARCHAR(256) DEFAULT NULL COMMENT '文本结果',
  `calc_time` DATETIME NOT NULL COMMENT '计算时间',
  `data_range_start` DATETIME DEFAULT NULL,
  `data_range_end` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kpi_id` (`kpi_id`),
  KEY `idx_tenant_calc` (`tenant_id`, `calc_time`)
) ENGINE=InnoDB COMMENT='KPI计算结果表';

-- ============================================================
-- 5. WORKFLOW ENGINE
-- ============================================================

CREATE TABLE `workflow_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `workflow_code` VARCHAR(128) NOT NULL COMMENT '工作流编码',
  `workflow_name` VARCHAR(128) NOT NULL COMMENT '工作流名称',
  `version` INT NOT NULL DEFAULT 1,
  `description` VARCHAR(512) DEFAULT NULL,
  `dag_definition` JSON NOT NULL COMMENT 'DAG定义(节点+边)',
  `trigger_type` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT 'manual/cron/event',
  `cron_expression` VARCHAR(64) DEFAULT NULL COMMENT 'Cron表达式',
  `timeout_seconds` INT DEFAULT 3600 COMMENT '超时时间(秒)',
  `max_retries` INT DEFAULT 3 COMMENT '最大重试次数',
  `retry_delay_seconds` INT DEFAULT 60 COMMENT '重试延迟(秒)',
  `status` VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/active/disabled',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_version` (`tenant_id`, `workflow_code`, `version`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB COMMENT='工作流定义表(版本化)';

-- ============================================================
-- 6. EXECUTION STATE & OBSERVABILITY ENGINE
-- ============================================================

CREATE TABLE `workflow_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `workflow_id` BIGINT NOT NULL,
  `workflow_version` INT NOT NULL,
  `run_id` VARCHAR(64) NOT NULL COMMENT '运行唯一ID(UUID)',
  `trigger_type` VARCHAR(32) NOT NULL COMMENT 'manual/cron/event',
  `triggered_by` BIGINT DEFAULT NULL COMMENT '触发者用户ID',
  `state` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/RETRYING/PAUSED/CANCELLED',
  `current_node_id` VARCHAR(128) DEFAULT NULL COMMENT '当前执行节点',
  `input_params` JSON DEFAULT NULL COMMENT '输入参数',
  `output_result` JSON DEFAULT NULL COMMENT '输出结果',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `total_tokens` BIGINT DEFAULT 0 COMMENT '总Token消耗',
  `total_cost` DECIMAL(10,4) DEFAULT 0 COMMENT '总成本(元)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_run_id` (`run_id`),
  KEY `idx_tenant_workflow` (`tenant_id`, `workflow_id`),
  KEY `idx_state` (`state`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB COMMENT='工作流运行记录表';

CREATE TABLE `workflow_node_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `run_id` VARCHAR(64) NOT NULL COMMENT '关联workflow_run.run_id',
  `node_id` VARCHAR(128) NOT NULL COMMENT 'DAG节点ID',
  `node_type` VARCHAR(32) NOT NULL COMMENT 'kpi_calc/ai_analysis/data_fetch/output/report',
  `node_name` VARCHAR(128) DEFAULT NULL,
  `state` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `input_data` JSON DEFAULT NULL,
  `output_data` JSON DEFAULT NULL,
  `error_message` TEXT DEFAULT NULL,
  `retry_count` INT DEFAULT 0,
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `duration_ms` BIGINT DEFAULT NULL,
  `tokens_used` BIGINT DEFAULT 0,
  `cost` DECIMAL(10,4) DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_run_id` (`run_id`),
  KEY `idx_node_state` (`node_id`, `state`)
) ENGINE=InnoDB COMMENT='工作流节点执行记录表';

CREATE TABLE `workflow_execution_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `run_id` VARCHAR(64) NOT NULL,
  `node_id` VARCHAR(128) DEFAULT NULL,
  `log_level` VARCHAR(16) NOT NULL DEFAULT 'INFO' COMMENT 'DEBUG/INFO/WARN/ERROR',
  `message` TEXT NOT NULL,
  `context_data` JSON DEFAULT NULL COMMENT '上下文数据',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_run_id` (`run_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB COMMENT='工作流执行日志表';

CREATE TABLE `workflow_state_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `run_id` VARCHAR(64) NOT NULL,
  `snapshot_name` VARCHAR(128) NOT NULL COMMENT '快照名称',
  `node_states` JSON NOT NULL COMMENT '所有节点状态',
  `global_state` JSON DEFAULT NULL COMMENT '全局状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_run_id` (`run_id`)
) ENGINE=InnoDB COMMENT='工作流状态快照表(用于恢复)';

-- ============================================================
-- 7. CONFIG CONSISTENCY ENGINE
-- ============================================================

CREATE TABLE `config_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `config_type` VARCHAR(32) NOT NULL COMMENT 'schema/kpi/workflow/prompt/report',
  `config_id` BIGINT NOT NULL COMMENT '配置项ID',
  `config_code` VARCHAR(128) NOT NULL COMMENT '配置编码',
  `version` INT NOT NULL COMMENT '版本号',
  `config_snapshot` JSON NOT NULL COMMENT '配置快照',
  `change_summary` VARCHAR(512) DEFAULT NULL COMMENT '变更说明',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_version` (`tenant_id`, `config_type`, `config_code`, `version`),
  KEY `idx_tenant_type` (`tenant_id`, `config_type`)
) ENGINE=InnoDB COMMENT='配置版本表';

CREATE TABLE `config_dependency_graph` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `source_type` VARCHAR(32) NOT NULL COMMENT '源配置类型',
  `source_code` VARCHAR(128) NOT NULL COMMENT '源配置编码',
  `source_version` INT NOT NULL,
  `target_type` VARCHAR(32) NOT NULL COMMENT '目标配置类型',
  `target_code` VARCHAR(128) NOT NULL COMMENT '目标配置编码',
  `target_version` INT NOT NULL,
  `dependency_type` VARCHAR(32) NOT NULL DEFAULT 'reference' COMMENT 'reference/composition/trigger',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dependency` (`tenant_id`, `source_type`, `source_code`, `source_version`, `target_type`, `target_code`, `target_version`),
  KEY `idx_source` (`source_type`, `source_code`),
  KEY `idx_target` (`target_type`, `target_code`)
) ENGINE=InnoDB COMMENT='配置依赖关系图';

CREATE TABLE `config_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `snapshot_name` VARCHAR(128) NOT NULL COMMENT '快照名称',
  `snapshot_version` VARCHAR(64) NOT NULL COMMENT '快照版本号',
  `description` VARCHAR(512) DEFAULT NULL,
  `full_snapshot` JSON NOT NULL COMMENT '完整系统配置快照',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_version` (`tenant_id`, `snapshot_version`)
) ENGINE=InnoDB COMMENT='系统配置快照表(用于回滚)';

-- ============================================================
-- 8. PROMPT & REPORT TEMPLATES
-- ============================================================

CREATE TABLE `prompt_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `prompt_code` VARCHAR(128) NOT NULL COMMENT 'Prompt编码',
  `prompt_name` VARCHAR(128) NOT NULL COMMENT 'Prompt名称',
  `version` INT NOT NULL DEFAULT 1,
  `schema_id` BIGINT DEFAULT NULL COMMENT '关联Schema',
  `schema_version` INT DEFAULT NULL,
  `prompt_type` VARCHAR(32) NOT NULL COMMENT 'nl2sql/analysis/report/summary',
  `system_prompt` TEXT NOT NULL COMMENT '系统Prompt',
  `user_prompt_template` TEXT NOT NULL COMMENT '用户Prompt模板(支持变量)',
  `output_schema` JSON DEFAULT NULL COMMENT '输出JSON Schema',
  `model_config` JSON DEFAULT NULL COMMENT '模型配置(temperature等)',
  `description` VARCHAR(512) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'active',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prompt_version` (`tenant_id`, `prompt_code`, `version`)
) ENGINE=InnoDB COMMENT='Prompt模板表(版本化)';

CREATE TABLE `report_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `template_code` VARCHAR(128) NOT NULL COMMENT '模板编码',
  `template_name` VARCHAR(128) NOT NULL COMMENT '模板名称',
  `version` INT NOT NULL DEFAULT 1,
  `output_format` VARCHAR(16) NOT NULL COMMENT 'word/ppt/pdf',
  `template_file_path` VARCHAR(512) NOT NULL COMMENT '模板文件路径(MinIO)',
  `schema_ids` JSON DEFAULT NULL COMMENT '关联Schema ID列表',
  `kpi_ids` JSON DEFAULT NULL COMMENT '关联KPI ID列表',
  `prompt_ids` JSON DEFAULT NULL COMMENT '关联Prompt ID列表',
  `variables` JSON DEFAULT NULL COMMENT '模板变量定义',
  `description` VARCHAR(512) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'active',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_version` (`tenant_id`, `template_code`, `version`)
) ENGINE=InnoDB COMMENT='报表模板表(版本化)';

-- ============================================================
-- 9. AI CONTROL & SAFETY ENGINE
-- ============================================================

CREATE TABLE `ai_policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `policy_code` VARCHAR(128) NOT NULL COMMENT '策略编码',
  `policy_name` VARCHAR(128) NOT NULL COMMENT '策略名称',
  `policy_type` VARCHAR(32) NOT NULL COMMENT 'global/tenant/user/workflow',
  `rules` JSON NOT NULL COMMENT '策略规则',
  `description` VARCHAR(512) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_code` (`tenant_id`, `policy_code`)
) ENGINE=InnoDB COMMENT='AI策略表';

CREATE TABLE `ai_execution_trace` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `trace_id` VARCHAR(64) NOT NULL COMMENT '追踪ID',
  `run_id` VARCHAR(64) DEFAULT NULL COMMENT '关联工作流运行ID',
  `node_id` VARCHAR(128) DEFAULT NULL,
  `ai_task_type` VARCHAR(32) NOT NULL COMMENT 'nl2sql/analysis/report',
  `input_prompt` TEXT NOT NULL COMMENT '输入Prompt',
  `prompt_tokens` INT DEFAULT 0,
  `completion_tokens` INT DEFAULT 0,
  `total_tokens` INT DEFAULT 0,
  `model_name` VARCHAR(64) DEFAULT NULL COMMENT '使用的模型',
  `model_config` JSON DEFAULT NULL COMMENT '模型参数',
  `raw_output` TEXT DEFAULT NULL COMMENT '原始输出',
  `validated_output` TEXT DEFAULT NULL COMMENT '验证后输出',
  `validation_passed` TINYINT DEFAULT NULL COMMENT '验证是否通过',
  `validation_errors` JSON DEFAULT NULL COMMENT '验证错误详情',
  `retry_count` INT DEFAULT 0,
  `latency_ms` BIGINT DEFAULT NULL COMMENT '响应延迟(毫秒)',
  `cost` DECIMAL(10,6) DEFAULT 0 COMMENT '本次调用成本',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/success/failed/validated',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trace_id` (`trace_id`),
  KEY `idx_tenant_run` (`tenant_id`, `run_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB COMMENT='AI执行追踪表';

CREATE TABLE `ai_sql_validation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(64) NOT NULL,
  `original_sql` TEXT NOT NULL COMMENT 'AI生成的原始SQL',
  `parsed_ast` JSON DEFAULT NULL COMMENT '解析后的AST',
  `validation_result` VARCHAR(16) NOT NULL COMMENT 'pass/fail',
  `validation_errors` JSON DEFAULT NULL COMMENT '校验错误',
  `corrected_sql` TEXT DEFAULT NULL COMMENT '修正后的SQL',
  `columns_referenced` JSON DEFAULT NULL COMMENT '引用的列',
  `tables_referenced` JSON DEFAULT NULL COMMENT '引用的表',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB COMMENT='AI SQL校验日志表';

-- ============================================================
-- 10. OUTPUT ENGINE
-- ============================================================

CREATE TABLE `report_output` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `report_name` VARCHAR(128) NOT NULL COMMENT '报表名称',
  `template_id` BIGINT DEFAULT NULL COMMENT '使用的模板ID',
  `run_id` VARCHAR(64) DEFAULT NULL COMMENT '关联工作流运行ID',
  `output_format` VARCHAR(16) NOT NULL COMMENT 'word/ppt/pdf',
  `file_path` VARCHAR(512) NOT NULL COMMENT '文件路径(MinIO)',
  `file_size` BIGINT DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'generating' COMMENT 'generating/ready/failed/expired',
  `generated_at` DATETIME DEFAULT NULL,
  `expires_at` DATETIME DEFAULT NULL COMMENT '过期时间',
  `download_count` INT DEFAULT 0,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_run_id` (`run_id`)
) ENGINE=InnoDB COMMENT='报表输出表';

-- ============================================================
-- 11. SYSTEM AUDIT
-- ============================================================

CREATE TABLE `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `username` VARCHAR(64) DEFAULT NULL,
  `action` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `resource_type` VARCHAR(64) NOT NULL COMMENT '资源类型',
  `resource_id` VARCHAR(128) DEFAULT NULL COMMENT '资源ID',
  `old_value` JSON DEFAULT NULL COMMENT '旧值',
  `new_value` JSON DEFAULT NULL COMMENT '新值',
  `ip_address` VARCHAR(64) DEFAULT NULL,
  `user_agent` VARCHAR(256) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_action` (`action`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB COMMENT='审计日志表';

-- ============================================================
-- 12. DEMO DATA
-- ============================================================

-- Default tenant
INSERT INTO `tenant` (`tenant_code`, `tenant_name`, `contact_name`, `contact_email`, `plan_type`, `max_users`, `max_datasets`, `max_ai_calls_per_day`)
VALUES ('default', '默认租户', 'Admin', 'admin@enterprise.com', 'enterprise', 100, 500, 10000);

-- Default roles
INSERT INTO `sys_role` (`tenant_id`, `role_code`, `role_name`, `description`, `is_system`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '系统超级管理员', 1),
(1, 'TENANT_ADMIN', '租户管理员', '租户管理员', 1),
(1, 'DATA_ANALYST', '数据分析师', '数据分析师', 0),
(1, 'REPORT_VIEWER', '报表查看者', '只读用户', 0);

-- Default admin user (password: admin123 BCrypt encoded)
INSERT INTO `sys_user` (`tenant_id`, `username`, `password`, `real_name`, `email`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@enterprise.com');

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- Default AI policy
INSERT INTO `ai_policy` (`tenant_id`, `policy_code`, `policy_name`, `policy_type`, `rules`, `description`) VALUES
(1, 'default_policy', '默认AI策略', 'tenant', '{"allow_sql_generation": true, "allow_schema_access": true, "allow_cross_dataset_join": false, "max_sql_complexity": 10, "allowed_sql_types": ["SELECT"], "require_where_clause": true, "max_result_rows": 10000}', '默认租户AI策略');
