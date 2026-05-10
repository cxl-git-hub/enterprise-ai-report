# 功能设计审查报告

> 审查时间: 2026-05-11
> 审查范围: 功能完整性、设计合理性、缺失功能

---

## 一、后端 stub 实现（功能空壳）

以下 Controller 方法只返回 `ApiResponse.success()` 或硬编码空数据，前端调用后不会有任何效果：

| # | 方法 | 问题 | 影响 |
|---|------|------|------|
| 1 | `AuthController.updateProfile()` | 空实现，不保存 | 个人中心编辑资料无效 |
| 2 | `AuthController.changePassword()` | 空实现，不校验旧密码 | 修改密码功能无效 |
| 3 | `AuthController.saveNotificationPreferences()` | 空实现 | 通知偏好设置无效 |
| 4 | `AuthController.getMyStats()` | 硬编码全返回0 | 个人中心统计永远是0 |
| 5 | `ConfigConsistencyServiceImpl.diffSnapshots()` | 返回空 changes | 快照对比功能无效 |
| 6 | `ReportOutputController.download()` | 返回 String URL 而非文件流 | 下载报表可能失败 |
| 7 | `DataSourceController.testConnection()` | 返回 Boolean 无详情 | 测试连接无延迟/版本信息 |

## 二、设计不合理之处

### 2.1 工作流不支持定时触发
- 后端 `WorkflowDefinition` 有 `cronExpression` 字段
- 前端 DAG 编辑器有调度表达式输入框
- **但没有 Cron Scheduler 实现** — 没有 Quartz/ScheduledExecutor 去实际触发定时任务
- 用户配置了 Cron 表达式也不会生效

### 2.2 数据源连接测试过于简单
- `DataSourceService.testConnection()` 只返回 `Boolean`
- 应返回 `{ success, message, latency, version }` 给前端展示详情
- 前端 DataSourceView 已经在展示 `latency` 和 `version`，但后端不提供

### 2.3 KPI 执行缺少日期范围传递
- 前端 KPI 执行对话框没有日期范围输入
- 后端 `KpiExecuteRequest` 有 `periodStart`/`periodEnd`
- 用户无法按时间段执行 KPI

### 2.4 工作流触发缺少参数输入
- 前端触发工作流时没有参数输入表单
- API 文档说支持 `{ dateRange, notifyEmail }` 参数
- 实际前端只调用 `workflowApi.trigger(record.id)` 不传任何参数

### 2.5 NL2SQL 缺少 Schema 上下文传递
- 前端 NL2SQL 页面发送 `query` 和 `dataset_ids`
- 但 Python 服务需要 Schema 的列定义来生成 SQL
- 没有把 Schema 的 `columnDefinitions` 传给 AI

### 2.6 AI 分析结果没有持久化
- AI 分析结果只在前端展示，不保存到数据库
- 用户刷新页面后分析结果丢失
- 应保存到 `report_output` 或专门的 `analysis_result` 表

### 2.7 报表模板缺少预览功能
- 用户创建报表模板后无法预览效果
- 应支持"预览"按钮生成示例报表

### 2.8 配置一致性校验不够严格
- 只校验 KPI→Schema 和 Prompt→Schema 引用
- 不校验 Workflow→KPI 引用（DAG 中的 kpiId 可能指向不存在的 KPI）
- 不校验 ReportTemplate→KPI 引用

## 三、缺失功能（商业级必须）

### 3.1 高优先级缺失

| # | 功能 | 说明 | 影响 |
|---|------|------|------|
| 1 | **工作流定时执行** | 需要 Cron Scheduler 按 cronExpression 自动触发 | 定时报表完全不可用 |
| 2 | **报表邮件发送** | 报表生成后自动发送到配置的邮箱 | 企业核心需求 |
| 3 | **数据源连接池** | 当前每次查询都新建连接，高并发会耗尽连接 | 生产环境必崩 |
| 4 | **SQL 查询超时控制** | NL2SQL 生成的 SQL 没有超时限制 | 恶意查询可拖垮数据库 |
| 5 | **操作确认对话框** | 删除操作缺少二次确认（部分页面有，部分没有） | 误删数据 |
| 6 | **批量操作** | 缺少批量删除、批量启用/禁用 | 管理效率低 |

### 3.2 中优先级缺失

| # | 功能 | 说明 |
|---|------|------|
| 7 | **API 限流** | 无 rate limiting，可被刷爆 |
| 8 | **WebSocket 实时通知** | 当前靠轮询，浪费资源 |
| 9 | **配置导入/导出** | 租户间迁移配置 |
| 10 | **工作流模板** | 预置常用工作流模板 |
| 11 | **KPI 趋势图** | KPI 应支持历史趋势可视化 |
| 12 | **数据血缘** | 追踪数据从源到报表的完整链路 |
| 13 | **报表版本管理** | 同一报表的历史版本对比 |
| 14 | **用户操作日志** | 区分审计日志和用户行为日志 |

### 3.3 低优先级缺失

| # | 功能 | 说明 |
|---|------|------|
| 15 | SSO/LDAP 集成 | 企业单点登录 |
| 16 | 多语言 i18n | 国际化支持 |
| 17 | API 文档 (Swagger) | 开发者集成 |
| 18 | 移动端适配 | 部分页面已做，但不完整 |
| 19 | 数据脱敏 | 敏感字段自动脱敏 |
| 20 | 灾备/高可用 | 多节点部署方案 |

## 四、已有功能需改进

### 4.1 仪表盘
- ❌ 缺少 AI 用量统计（Token消耗、费用趋势）
- ❌ 缺少时间范围筛选（今天/本周/本月）
- ❌ 缺少环比对比（vs 上周/上月）
- ✅ 已有统计卡片 + 运行列表 + 状态分布图

### 4.2 数据源管理
- ❌ 测试连接返回 Boolean，应返回详情
- ❌ 无连接池配置
- ❌ 无数据同步调度
- ✅ 已有文件上传、CRUD、连接测试

### 4.3 Schema 管理
- ❌ 版本历史只是时间线展示，无 diff 对比
- ❌ 无 Schema 模板库
- ✅ 已有可视化列编辑器、AI建议、版本历史

### 4.4 KPI 管理
- ❌ 执行时无日期范围选择
- ❌ 无 KPI 趋势图
- ❌ 无 KPI 告警阈值
- ✅ 已有 DSL 编辑器、AI建议、执行、版本

### 4.5 工作流
- ❌ 定时触发未实现
- ❌ 触发时无参数输入
- ✅ 已有 DAG 编辑器、并行执行、状态追踪、恢复

### 4.6 AI 服务
- ❌ NL2SQL 缺少完整 Schema 上下文
- ❌ 分析结果不持久化
- ✅ 已有 NL2SQL、分析、追踪、安全引擎

### 4.7 输出引擎
- ❌ 报表下载可能不工作（返回URL而非流）
- ❌ 无邮件发送
- ✅ 已有多格式生成、MinIO存储

---

## 五、建议修复优先级

### P0（立即修复 - 功能不可用）
1. 后端 stub 方法实现（profile/password/stats）
2. 工作流触发参数传递
3. 报表下载改为文件流

### P1（尽快修复 - 功能不完整）
4. 工作流定时执行（Cron Scheduler）
5. 数据源连接池
6. KPI 执行日期范围
7. NL2SQL Schema 上下文传递

### P2（计划修复 - 体验优化）
8. AI 仪表盘统计
9. 快照对比功能
10. 报表预览
11. 批量操作
