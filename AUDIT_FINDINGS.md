# 审查发现与修复记录

> 最后更新: 2026-05-11

## ✅ 已修复问题

### 1. 数据库Schema与Java实体不匹配（严重 - P0）
- **问题**: `database/schema.sql` 使用 `source_name`, `kpi_name` 等列名，但Java实体使用 `name`, `type` 等简单字段名
- **修复**: 重写 `database/schema.sql` 使其完全匹配Java实体定义
- **影响**: 修复前系统无法启动

### 2. Controller方法引用不存在的实体方法（P0）
- **问题**: `DashboardController`, `WorkflowExecutionServiceImpl`, `WorkflowCronScheduler` 调用 `getWorkflowName()` 但实体只有 `getName()`
- **修复**: 将所有 `getWorkflowName()` 改为 `getName()`

### 3. WorkflowRun状态查询类型不匹配（P0）
- **问题**: `WorkflowRunController` 使用 `wrapper.eq(WorkflowRun::getState, status)` 但 `state` 是枚举类型，`status` 是小写字符串
- **修复**: 添加 `WorkflowState.valueOf(status.toUpperCase())` 转换

### 4. DashboardController查询workflow名称（P1）
- **问题**: `getRecentRuns` 直接引用 `run.getWorkflowName()` 但WorkflowRun没有此字段
- **修复**: 批量查询WorkflowDefinition获取名称映射

### 5. 前后端API路径与字段映射
- 前端API模块已包含 `mapWorkflowFromBackend`, `mapWorkflowToBackend`, `mapReportFromBackend` 等映射函数
- 字段名转换: `dagDefinition` ↔ `nodes`, `cronExpression` ↔ `schedule`
- 数据源字段: `database` → `databaseName`, `password` → `encryptedPassword`

## ✅ 已实现功能

### P1功能
| 功能 | 状态 | 文件 |
|------|------|------|
| API限流 | ✅ | `config/RateLimitConfig.java` - Redis滑动窗口，100次/分钟 |
| WebSocket通知 | ✅ | `config/WebSocketConfig.java` + `service/WebSocketNotificationService.java` |
| 配置导入导出 | ✅ | `controller/ConfigConsistencyController.java` - GET/POST `/config/export`, `/config/import` |
| KPI趋势图 | ✅ | `controller/KpiController.java` - GET `/kpis/{id}/trend` |
| 工作流定时执行 | ✅ | `engine/scheduler/WorkflowCronScheduler.java` - 支持5/6字段Cron表达式 |

### P2功能
| 功能 | 状态 | 文件 |
|------|------|------|
| Swagger文档 | ✅ | `config/OpenApiConfig.java` + springdoc-openapi依赖 |
| 数据脱敏 | ✅ | `util/DataMaskingUtil.java` - 手机/邮箱/身份证/银行卡脱敏 |

### 其他已实现
- **27张数据库表** - 完整DDL + 索引 + 外键逻辑
- **18个Java Controller** - 完整CRUD + 业务逻辑
- **26个Vue页面** - 完整前端管理界面
- **JWT双Token认证** - AccessToken + RefreshToken
- **多租户隔离** - MyBatis Plus租户拦截器
- **AI安全引擎** - SQL验证 + Prompt注入防护 + 输出校验
- **深色模式** - 30+组件覆盖
- **键盘快捷键** - Ctrl+K搜索, Ctrl+D仪表盘等
- **数据导出** - CSV/JSON/Markdown格式

## 待优化项（低优先级）

1. **SSO/LDAP集成** - 企业单点登录
2. **i18n多语言** - 国际化支持
3. **移动端适配** - 部分页面响应式不完整
4. **数据血缘** - 完整数据链路追踪
5. **报表版本管理** - 同一报表历史版本对比
