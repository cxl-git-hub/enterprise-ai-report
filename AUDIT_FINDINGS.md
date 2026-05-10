# 审查发现的前后端不匹配问题

## 🔴 严重问题（会导致功能完全不可用）

### 1. API路径不匹配
| 前端API | 后端Controller路径 | 问题 |
|---------|-------------------|------|
| `/prompts` | `/api/prompt-templates` | ❌ 路径不同 |
| `POST /workflows/{id}/trigger` | `POST /workflows/trigger` (body含workflowId) | ❌ 路由方式不同 |
| `GET /datasets/{id}/preview` | 不存在 | ❌ 缺失endpoint |
| `POST /datasets/{id}/columns/refresh` | `POST /datasets/{id}/sync-columns` | ❌ 路径不同 |

### 2. 字段名不匹配
| 模块 | 前端字段 | 后端字段 | 问题 |
|------|----------|----------|------|
| DataSource | `database`, `password` | `databaseName`, `encryptedPassword` | ❌ |
| DataSource | `lastTestStatus` | `lastTestResult` | ❌ |
| Dataset | `datasourceName` | 未返回 | ❌ |
| Schema | `schemaCode`, `schemaName`, `columnDefinitions` | `name`, `columns` | ❌ |
| Schema | `datasetName` | 未返回 | ❌ |
| Workflow | `nodes`, `schedule` | `dagDefinition`, `cronExpression` | ❌ |
| WorkflowRun | `workflowName` | 未返回 | ❌ |
| Workflow trigger | `POST /{id}/trigger` | `POST /trigger` + body | ❌ |
| ReportOutput | `name`, `format` | `reportName`, `outputFormat` | ❌ |
| AuditLog | `resourceName`, `details` | `resourceId`, `oldValue/newValue` | ❌ |
| Auth | `displayName` | `realName` | ❌ |
| Tenant | `name`, `code` | `tenantName`, `tenantCode` | ❌ |
| Notification | `read` | `isRead` | ❌ |
