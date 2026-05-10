# API 接口文档

> 最后更新: 2026-05-11

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Bearer Token
- **Content-Type**: `application/json`
- **多租户**: 通过 Header `X-Tenant-Id` 传递租户ID
- **API限流**: 100次/分钟/租户 (Redis滑动窗口)
- **Swagger文档**: `http://localhost:8080/swagger-ui.html`

## 通用响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## 分页响应格式

```json
{
  "code": 200,
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "pages": 5
  }
}
```

---

## 1. 认证模块 (Auth)

### 1.1 登录

```
POST /api/auth/login
```

**请求体**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "expiresIn": 7200000,
    "userId": 1,
    "tenantId": 1,
    "username": "admin",
    "realName": "系统管理员"
  }
}
```

### 1.2 注册

```
POST /api/auth/register
```

**请求体**:
```json
{
  "username": "analyst01",
  "password": "pass123456",
  "realName": "张三",
  "email": "zhangsan@company.com",
  "phone": "13800138000",
  "tenantId": 1
}
```

### 1.3 刷新Token

```
POST /api/auth/refresh
```

**请求体**:
```json
{
  "refreshToken": "eyJhbG..."
}
```

### 1.4 获取当前用户

```
GET /api/auth/me
Authorization: Bearer {accessToken}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@enterprise.com",
    "realName": "系统管理员",
    "tenantId": 1,
    "roles": ["SUPER_ADMIN"],
    "permissions": ["user:manage", "config:manage", "workflow:execute"]
  }
}
```

---

## 2. 租户管理 (Tenant)

### 2.1 租户列表

```
GET /api/tenants?page=1&size=20
```

### 2.2 创建租户

```
POST /api/tenants
```

**请求体**:
```json
{
  "tenantCode": "company_a",
  "tenantName": "A公司",
  "contactName": "李四",
  "contactEmail": "lisi@a.com",
  "planType": "enterprise",
  "maxUsers": 50,
  "maxDatasets": 200,
  "maxAiCallsPerDay": 5000
}
```

### 2.3 更新租户

```
PUT /api/tenants/{id}
```

### 2.4 删除租户

```
DELETE /api/tenants/{id}
```

---

## 3. 用户管理 (User)

### 3.1 用户列表

```
GET /api/users?page=1&size=20&keyword=zhang
```

### 3.2 创建用户

```
POST /api/users
```

**请求体**:
```json
{
  "username": "analyst01",
  "password": "pass123456",
  "realName": "张三",
  "email": "zhangsan@company.com",
  "phone": "13800138000"
}
```

### 3.3 分配角色

```
POST /api/users/{id}/roles
```

**请求体**:
```json
{
  "roleIds": [1, 3]
}
```

---

## 4. 数据源管理 (DataSource)

### 4.1 数据源列表

```
GET /api/datasources?page=1&size=20
```

### 4.2 创建数据源

```
POST /api/datasources
```

**请求体**:
```json
{
  "sourceName": "生产数据库",
  "sourceType": "mysql",
  "connectionConfig": {
    "host": "192.168.1.100",
    "port": 3306,
    "database": "production",
    "username": "reader",
    "password": "encrypted_password"
  },
  "description": "生产环境只读副本"
}
```

### 4.3 测试连接

```
POST /api/datasources/{id}/test
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "success": true,
    "message": "Connection successful",
    "latency": 45,
    "version": "8.0.36"
  }
}
```

---

## 5. 数据集管理 (Dataset)

### 5.1 数据集列表

```
GET /api/datasets?page=1&size=20
```

### 5.2 创建数据集

```
POST /api/datasets
```

**请求体**:
```json
{
  "sourceId": 1,
  "datasetName": "订单表",
  "datasetType": "table",
  "description": "电商订单主表"
}
```

### 5.3 获取列信息

```
GET /api/datasets/{id}/columns
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "columnName": "order_id",
      "columnType": "number",
      "displayName": "订单ID",
      "isPrimaryKey": true,
      "description": "订单唯一标识"
    },
    {
      "columnName": "amount",
      "columnType": "number",
      "displayName": "金额",
      "description": "订单金额(元)"
    }
  ]
}
```

---

## 6. Schema管理

### 6.1 Schema列表

```
GET /api/schemas?page=1&size=20
```

### 6.2 创建Schema

```
POST /api/schemas
```

**请求体**:
```json
{
  "schemaCode": "order_schema",
  "schemaName": "订单数据Schema",
  "datasetId": 1,
  "columnDefinitions": [
    {
      "name": "order_id",
      "type": "number",
      "description": "订单ID",
      "isPrimaryKey": true
    },
    {
      "name": "amount",
      "type": "number",
      "description": "订单金额"
    },
    {
      "name": "status",
      "type": "string",
      "description": "订单状态: pending/paid/shipped/completed"
    },
    {
      "name": "created_at",
      "type": "date",
      "description": "创建时间"
    }
  ],
  "validationRules": {
    "required": ["order_id", "amount"],
    "unique": ["order_id"]
  }
}
```

### 6.3 版本历史

```
GET /api/schemas/{id}/versions
```

---

## 7. KPI管理

### 7.1 KPI列表

```
GET /api/kpis?page=1&size=20
```

### 7.2 创建KPI

```
POST /api/kpis
```

**请求体**:
```json
{
  "kpiCode": "order_count",
  "kpiName": "订单总数",
  "schemaId": 1,
  "schemaVersion": 1,
  "kpiType": "count",
  "expression": "COUNT(order_id)",
  "dimensions": ["status", "created_at"],
  "filters": [
    {
      "field": "status",
      "operator": "in",
      "value": ["paid", "shipped", "completed"]
    }
  ],
  "unit": "单",
  "description": "已完成订单总数",
  "businessExplanation": "统计状态为已支付、已发货、已完成的订单数量"
}
```

### 7.3 执行KPI计算

```
POST /api/kpis/{id}/execute
```

**请求体**:
```json
{
  "dateRange": {
    "start": "2024-01-01",
    "end": "2024-12-31"
  },
  "groupBy": ["status"]
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "kpiCode": "order_count",
    "result": 15234,
    "dimensions": {
      "status": [
        {"value": "paid", "count": 5200},
        {"value": "shipped", "count": 4800},
        {"value": "completed", "count": 5234}
      ]
    },
    "calculatedAt": "2024-05-10T12:00:00"
  }
}
```

---

## 8. Prompt模板管理

### 8.1 Prompt列表

```
GET /api/prompts?page=1&size=20
```

### 8.2 创建Prompt

```
POST /api/prompts
```

**请求体**:
```json
{
  "promptCode": "nl2sql_order",
  "promptName": "订单查询NL2SQL",
  "schemaId": 1,
  "schemaVersion": 1,
  "promptType": "nl2sql",
  "systemPrompt": "你是一个SQL专家，根据用户自然语言问题生成MySQL查询语句。",
  "userPromptTemplate": "表结构:\n{{schema_context}}\n\n用户问题: {{user_query}}\n\n请生成SQL查询语句:",
  "outputSchema": {
    "type": "object",
    "properties": {
      "sql": {"type": "string"},
      "explanation": {"type": "string"}
    }
  },
  "modelConfig": {
    "temperature": 0.1,
    "maxTokens": 2000
  }
}
```

---

## 9. 报表模板管理

### 9.1 报表模板列表

```
GET /api/report-templates?page=1&size=20
```

### 9.2 创建报表模板

```
POST /api/report-templates
```

**请求体**:
```json
{
  "templateCode": "monthly_report",
  "templateName": "月度经营报表",
  "outputFormat": "pdf",
  "schemaIds": [1, 2],
  "kpiIds": [1, 2, 3],
  "variables": {
    "report_title": "月度经营分析报告",
    "company_name": "XX科技有限公司"
  }
}
```

---

## 10. 工作流管理 (Workflow)

### 10.1 工作流列表

```
GET /api/workflows?page=1&size=20
```

### 10.2 创建工作流

```
POST /api/workflows
```

**请求体**:
```json
{
  "workflowCode": "monthly_analysis",
  "workflowName": "月度分析流程",
  "description": "每月自动生成经营分析报表",
  "dagDefinition": {
    "nodes": [
      {
        "id": "fetch_data",
        "name": "数据获取",
        "type": "data_fetch",
        "params": {"datasetId": 1}
      },
      {
        "id": "calc_kpi",
        "name": "KPI计算",
        "type": "kpi_calc",
        "params": {"kpiIds": [1, 2, 3]}
      },
      {
        "id": "ai_analysis",
        "name": "AI分析",
        "type": "ai_analysis",
        "params": {"schemaId": 1, "promptId": 1}
      },
      {
        "id": "gen_report",
        "name": "生成报表",
        "type": "output",
        "params": {"templateId": 1, "format": "pdf"}
      }
    ],
    "edges": [
      {"source": "fetch_data", "target": "calc_kpi"},
      {"source": "calc_kpi", "target": "ai_analysis"},
      {"source": "ai_analysis", "target": "gen_report"}
    ]
  },
  "triggerType": "manual",
  "timeoutSeconds": 3600,
  "maxRetries": 3
}
```

### 10.3 触发工作流

```
POST /api/workflows/{id}/trigger
```

**请求体**:
```json
{
  "dateRange": "2024-04",
  "notifyEmail": "admin@company.com"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "runId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "workflowId": 1,
    "status": "running",
    "triggerType": "manual",
    "startedAt": "2024-05-10T12:00:00"
  }
}
```

---

## 11. 工作流运行 (WorkflowRun)

### 11.1 运行列表

```
GET /api/workflow-runs?page=1&size=20&workflowId=1&status=failed
```

### 11.2 运行详情

```
GET /api/workflow-runs/{id}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "runId": "a1b2c3d4-...",
    "workflowId": 1,
    "status": "success",
    "startedAt": "2024-05-10T12:00:00",
    "finishedAt": "2024-05-10T12:05:30",
    "duration": 330000,
    "totalTokens": 15234,
    "totalCost": 0.1523,
    "nodeRuns": [
      {
        "id": 1,
        "nodeId": "fetch_data",
        "nodeName": "数据获取",
        "nodeType": "data_fetch",
        "status": "success",
        "startedAt": "2024-05-10T12:00:00",
        "finishedAt": "2024-05-10T12:00:30",
        "duration": 30000,
        "tokensUsed": 0,
        "cost": 0
      },
      {
        "id": 2,
        "nodeId": "calc_kpi",
        "nodeName": "KPI计算",
        "nodeType": "kpi_calc",
        "status": "success",
        "startedAt": "2024-05-10T12:00:30",
        "finishedAt": "2024-05-10T12:01:00",
        "duration": 30000,
        "tokensUsed": 0,
        "cost": 0
      },
      {
        "id": 3,
        "nodeId": "ai_analysis",
        "nodeName": "AI分析",
        "nodeType": "ai_analysis",
        "status": "success",
        "startedAt": "2024-05-10T12:01:00",
        "finishedAt": "2024-05-10T12:04:00",
        "duration": 180000,
        "tokensUsed": 12000,
        "cost": 0.12
      }
    ],
    "stateSnapshots": [
      {"state": "fetch_data", "completedNodes": "fetch_data", "timestamp": "2024-05-10T12:00:30"},
      {"state": "calc_kpi", "completedNodes": "fetch_data,calc_kpi", "timestamp": "2024-05-10T12:01:00"}
    ]
  }
}
```

### 11.3 从失败处恢复

```
POST /api/workflow-runs/{id}/resume
```

---

## 12. 配置一致性 (Config Consistency)

### 12.1 一致性校验

```
POST /api/config/validate
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "valid": false,
    "errors": [
      {
        "type": "kpi",
        "message": "KPI '订单转化率' 引用了不存在的 Schema (ID: 99)",
        "refId": "15",
        "refName": "订单转化率"
      }
    ],
    "warnings": [
      {
        "type": "schema",
        "message": "Schema '测试Schema' 未被任何 KPI 或 Prompt 引用",
        "refId": "5",
        "refName": "测试Schema"
      }
    ]
  }
}
```

### 12.2 依赖关系图

```
GET /api/config/dependency-graph
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "nodes": [
      {"id": "schema_1", "name": "订单Schema", "type": "schema", "dependencies": []},
      {"id": "kpi_1", "name": "订单总数", "type": "kpi", "dependencies": ["schema_1"]},
      {"id": "workflow_1", "name": "月度分析", "type": "workflow", "dependencies": ["kpi_1"]}
    ],
    "edges": [
      {"from": "schema_1", "to": "kpi_1", "type": "reference"},
      {"from": "kpi_1", "to": "workflow_1", "type": "reference"}
    ]
  }
}
```

### 12.3 创建快照

```
POST /api/config/snapshots
```

**请求体**:
```json
{
  "name": "v2.1.0发布前备份",
  "description": "版本发布前的完整配置备份"
}
```

### 12.4 快照列表

```
GET /api/config/snapshots?page=1&pageSize=20
```

### 12.5 恢复快照

```
POST /api/config/snapshots/{id}/restore
```

### 12.6 快照对比

```
GET /api/config/snapshots/diff?id1=1&id2=2
```

---

## 13. AI服务 (AI Service)

### 13.1 NL2SQL

```
POST /api/ai/nl2sql
```

**请求体**:
```json
{
  "question": "上个月销售额最高的前10个产品是什么？",
  "schemaId": 1,
  "maxRows": 100
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "sql": "SELECT p.product_name, SUM(o.amount) as total_sales FROM orders o JOIN products p ON o.product_id = p.id WHERE o.created_at >= '2024-04-01' AND o.created_at < '2024-05-01' GROUP BY p.product_name ORDER BY total_sales DESC LIMIT 10",
    "explanation": "查询上个月(2024年4月)每个产品的销售总额，按销售额降序排列，取前10名",
    "validation": {
      "passed": true,
      "errors": []
    },
    "result": [
      {"product_name": "iPhone 15", "total_sales": 1523400},
      {"product_name": "MacBook Pro", "total_sales": 987600}
    ],
    "traceId": "trace-abc123",
    "tokensUsed": 2340,
    "cost": 0.0234
  }
}
```

### 13.2 数据分析

```
POST /api/ai/analysis
```

**请求体**:
```json
{
  "datasetId": 1,
  "analysisType": "trend",
  "question": "分析最近6个月的销售趋势，找出异常波动",
  "dateRange": {
    "start": "2023-11-01",
    "end": "2024-04-30"
  }
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "findings": [
      {
        "title": "2月销售额异常下降",
        "description": "2024年2月销售额环比下降35%，主要原因是春节假期导致物流中断",
        "severity": "high",
        "impact": "约损失200万销售额"
      },
      {
        "title": "3月强势反弹",
        "description": "3月销售额环比增长52%，超过历史峰值",
        "severity": "info",
        "impact": "创历史新高"
      }
    ],
    "summary": "近6个月销售呈V型走势，2月触底后3月强势反弹...",
    "charts": [
      {
        "type": "line",
        "title": "月度销售趋势",
        "data": {"labels": ["Nov", "Dec", "Jan", "Feb", "Mar", "Apr"], "values": [100, 110, 105, 68, 103, 115]}
      }
    ],
    "traceId": "trace-def456",
    "tokensUsed": 4500,
    "cost": 0.045
  }
}
```

### 13.3 生成报表

```
POST /api/ai/report/generate
```

**请求体**:
```json
{
  "templateId": 1,
  "workflowRunId": 5,
  "outputFormat": "pdf",
  "variables": {
    "report_month": "2024年4月"
  }
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "reportId": 42,
    "status": "generating",
    "estimatedTime": 30
  }
}
```

### 13.4 执行追踪列表

```
GET /api/ai/traces?page=1&size=20&aiTaskType=nl2sql
```

### 13.5 执行追踪详情

```
GET /api/ai/traces/{traceId}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "traceId": "trace-abc123",
    "aiTaskType": "nl2sql",
    "inputPrompt": "表结构:\nCREATE TABLE orders...\n\n用户问题: 上个月销售额最高的...",
    "rawOutput": "```sql\nSELECT p.product_name...\n```",
    "validatedOutput": "SELECT p.product_name, SUM(o.amount)...",
    "validationPassed": true,
    "validationErrors": [],
    "modelName": "gpt-4o",
    "promptTokens": 1200,
    "completionTokens": 1140,
    "totalTokens": 2340,
    "latency": 3200,
    "cost": 0.0234,
    "retryCount": 0,
    "status": "success",
    "createdAt": "2024-05-10T12:01:00"
  }
}
```

---

## 14. 报表输出 (Report Output)

### 14.1 报表列表

```
GET /api/reports?page=1&size=20
```

### 14.2 下载报表

```
GET /api/reports/{id}/download
```

响应: 文件流 (application/pdf / application/vnd.openxmlformats-officedocument...)

---

## 15. 审计日志 (Audit Log)

### 15.1 日志列表

```
GET /api/audit-logs?page=1&size=20&action=login&startDate=2024-05-01&endDate=2024-05-10
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": 1,
        "userId": 1,
        "username": "admin",
        "action": "login",
        "resourceType": "auth",
        "ipAddress": "192.168.1.100",
        "createdAt": "2024-05-10T09:00:00"
      },
      {
        "id": 2,
        "userId": 1,
        "username": "admin",
        "action": "update",
        "resourceType": "kpi",
        "resourceId": "5",
        "oldValue": {"kpiName": "旧名称"},
        "newValue": {"kpiName": "新名称"},
        "createdAt": "2024-05-10T09:15:00"
      }
    ],
    "total": 156
  }
}
```

---

## 错误码

| HTTP状态码 | 说明 |
|-----------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 / Token过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突(如用户名已存在) |
| 500 | 服务器内部错误 |

## 通用错误响应

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {"field": "username", "message": "用户名不能为空"},
    {"field": "password", "message": "密码长度不能少于6位"}
  ]
}
```

---

## 16. 仪表盘 (Dashboard) 🆕

### 16.1 统计概览

```
GET /api/dashboard/stats
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "datasourceCount": 12,
    "kpiCount": 45,
    "workflowCount": 8,
    "reportCount": 156
  }
}
```

### 16.2 最近运行

```
GET /api/dashboard/recent-runs?limit=5
```

### 16.3 运行状态分布

```
GET /api/dashboard/run-status-distribution
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {"status": "success", "count": 120},
    {"status": "failed", "count": 15},
    {"status": "running", "count": 3}
  ]
}
```

---

## 17. 个人中心 (Auth Profile) 🆕

### 17.1 更新个人资料

```
PUT /api/auth/profile
Authorization: Bearer {accessToken}
```

**请求体**:
```json
{
  "displayName": "张三",
  "email": "zhangsan@company.com",
  "phone": "13800138000",
  "locale": "zh-CN"
}
```

### 17.2 修改密码

```
PUT /api/auth/password
Authorization: Bearer {accessToken}
```

**请求体**:
```json
{
  "oldPassword": "oldPass123",
  "newPassword": "newPass456"
}
```

### 17.3 通知偏好

```
PUT /api/auth/notification-preferences
Authorization: Bearer {accessToken}
```

**请求体**:
```json
{
  "workflowComplete": true,
  "workflowFailed": true,
  "reportReady": true,
  "systemAlert": false
}
```

### 17.4 我的统计

```
GET /api/auth/my-stats
Authorization: Bearer {accessToken}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "kpiCount": 12,
    "workflowCount": 5,
    "reportCount": 38
  }
}
```

---

## 18. 通知管理 (Notifications) 🆕

### 18.1 通知列表

```
GET /api/notifications?limit=20&type=error&read=false
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": "1",
      "type": "error",
      "message": "工作流「月度分析」执行失败: 节点 ai_analysis 超时",
      "read": false,
      "createdAt": "2024-05-10T15:30:00",
      "link": "/workflow/run/123"
    }
  ]
}
```

### 18.2 标记已读

```
PUT /api/notifications/{id}/read
```

### 18.3 全部已读

```
PUT /api/notifications/read-all
```

### 18.4 清空通知

```
PUT /api/notifications/clear
```

### 18.5 删除通知

```
DELETE /api/notifications/{id}
```

---

## 19. 系统设置 (Settings) 🆕

### 19.1 获取设置

```
GET /api/settings
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "appearance": {
      "theme": "light",
      "primaryColor": "#1677ff",
      "sidebarPosition": "left",
      "enableAnimation": true,
      "compactMode": false
    },
    "ai": {
      "model": "gpt-4",
      "baseUrl": "https://api.openai.com/v1",
      "temperature": 0.7,
      "maxTokens": 4096,
      "timeout": 60,
      "dailyLimit": 10000
    },
    "security": {
      "sessionTimeout": 120,
      "twoFactor": false
    },
    "advanced": {
      "dataRetention": 90,
      "maxConcurrentWorkflows": 5,
      "maxReportStorage": 1024,
      "debugMode": false
    }
  }
}
```

### 19.2 保存外观设置

```
PUT /api/settings/appearance
```

### 19.3 保存AI配置

```
PUT /api/settings/ai
```

### 19.4 保存通知设置

```
PUT /api/settings/notifications
```

### 19.5 保存安全设置

```
PUT /api/settings/security
```

### 19.6 保存高级设置

```
PUT /api/settings/advanced
```

### 19.7 清除缓存

```
PUT /api/settings/clear-cache
```

---

## 20. 文件上传 (DataSource Upload) 🆕

### 20.1 上传数据文件

```
POST /api/datasources/upload
Content-Type: multipart/form-data
```

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 数据源名称 |
| description | string | 否 | 描述 |
| file | File | 是 | 文件(Excel/CSV/JSON) |

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 42,
    "name": "Q1销售数据",
    "type": "file",
    "description": "上传文件: sales_q1.xlsx",
    "status": 1
  }
}
```

---

## 21. AI建议 (AI Suggestions) 🆕

### 21.1 建议列定义

```
POST /api/ai/suggest-columns
```

**请求体**:
```json
{
  "schema_name": "订单数据",
  "description": "电商订单表",
  "dataset_id": "1"
}
```

**响应**:
```json
{
  "data": {
    "columns": [
      {"name": "id", "type": "bigint", "nullable": false, "description": "主键ID", "businessMeaning": "订单唯一标识"},
      {"name": "amount", "type": "decimal", "nullable": false, "description": "订单金额", "businessMeaning": "订单总金额(元)"}
    ]
  }
}
```

### 21.2 建议KPI表达式

```
POST /api/ai/suggest-expression
```

**请求体**:
```json
{
  "kpi_name": "日均订单金额",
  "description": "每天的平均订单金额",
  "schema_id": "1",
  "aggregation_type": "avg"
}
```

### 21.3 优化提示词

```
POST /api/ai/optimize-prompt
```

**请求体**:
```json
{
  "name": "订单分析NL2SQL",
  "description": "将自然语言转为SQL查询",
  "category": "nl2sql",
  "current_template": "你是一个SQL专家..."
}
```

### 21.4 生成报表模板

```
POST /api/ai/generate-report-template
```

**请求体**:
```json
{
  "name": "月度经营分析报告",
  "description": "包含销售、利润、用户增长等指标",
  "format": "pdf"
}
```
