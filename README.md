# Enterprise AI Automated Reporting Platform

企业级AI自动化报表平台 — 从数据到洞察，全流程自动化

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Vue 3 + Ant Design Vue)         │
│  Dashboard │ DataHub │ Config │ Workflow │ AI │ Output │ Audit│
│  Profile │ Settings │ Notifications │ i18n │ Dark Mode       │
└────────────────────────┬────────────────────────────────────┘
                         │ REST API
┌────────────────────────┴────────────────────────────────────┐
│                Backend (Spring Boot 3 + MyBatis Plus)         │
│  Auth+RBAC │ DataHub │ Config │ KPI Engine │ Workflow Engine │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Config Consistency Engine │ Execution Observability  │   │
│  │  Notification Service │ Settings Service              │   │
│  │  Data Quality │ Alert Rules │ Report Scheduling       │   │
│  │  Data Lineage │ Data Security (Masking + RLS)         │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │ gRPC/HTTP
┌────────────────────────┴────────────────────────────────────┐
│              AI Service (FastAPI + LangGraph)                 │
│  NL2SQL │ Analysis │ Report Gen │ Safety Engine │ Policy     │
│  Suggestions (Schema/KPI/Prompt/Report Template AI)          │
│  Confidence Scorer │ Data Citation │ Multi-turn Chat         │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────┴────────────────────────────────────┐
│                    Storage Layer                              │
│  MySQL 8 │ Redis 7 │ MinIO                                   │
└─────────────────────────────────────────────────────────────┘
```

## 📦 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.2, Spring Security + JWT, MyBatis Plus 3.5 |
| AI服务 | Python 3.10+, FastAPI, LangGraph, SQLAlchemy (async) |
| 前端 | Vue 3, TypeScript, Vite 5, Pinia, Ant Design Vue 4 |
| 存储 | MySQL 8, Redis 7, MinIO |
| 部署 | Docker Compose |

## 🚀 快速启动

### 前置条件
- Docker & Docker Compose
- (可选) Node.js 18+, Java 17+, Python 3.10+ 用于本地开发

### 一键启动

```bash
# 克隆项目
cd enterprise-ai-report

# 配置环境变量
cp ai-service-python/.env.example ai-service-python/.env
# 编辑 .env 文件，配置 LLM_API_KEY 等

# 启动所有服务
cd docker
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:3000 | Vue 3 管理后台 |
| 后端API | http://localhost:8080 | Spring Boot REST API |
| AI服务 | http://localhost:8081 | FastAPI AI服务 |
| MinIO控制台 | http://localhost:9001 | 对象存储管理 |

### 默认账号
- 用户名: `admin`
- 密码: `admin123`

## 📁 项目结构

```
enterprise-ai-report/
├── backend-java/              # Java后端
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/enterprise/report/
│       ├── config/            # 配置类
│       ├── controller/        # REST控制器 (25个)
│       ├── dto/               # 数据传输对象
│       ├── engine/            # 核心引擎
│       │   ├── consistency/   # 配置一致性引擎
│       │   ├── kpi/           # KPI DSL求值器
│       │   ├── output/        # 报表生成器 + 免责声明注入
│       │   └── workflow/      # DAG工作流引擎(支持并行执行)
│       ├── entity/            # 实体类 (31个)
│       ├── enums/             # 枚举
│       ├── exception/         # 异常处理
│       ├── mapper/            # MyBatis Mapper (31个)
│       ├── safety/            # 安全层 (SQL/数据脱敏/行级权限)
│       ├── security/          # JWT安全层
│       ├── service/           # 业务服务 (21个)
│       └── util/              # 工具类
├── ai-service-python/         # Python AI服务
│   ├── requirements.txt
│   ├── Dockerfile
│   └── app/
│       ├── api/               # API路由 (含 suggestions.py)
│       ├── core/              # 核心配置
│       ├── engines/           # LangGraph引擎
│       │   ├── nl2sql/        # NL2SQL工作流
│       │   ├── analysis/      # 数据分析工作流
│       │   └── report/        # 报表生成工作流
│       ├── models/            # ORM模型
│       ├── prompts/           # Prompt模板
│       ├── safety/            # AI安全层
│       ├── schemas/           # Pydantic Schema
│       ├── services/          # 业务服务 (含 confidence_scorer.py)
│       └── utils/             # 工具函数(导出等)
├── frontend-vue/              # Vue 3 前端
│   ├── package.json
│   ├── Dockerfile
│   └── src/
│       ├── api/               # API模块 (14个)
│       ├── components/        # 公共组件
│       │   └── common/        # AiDisclaimer, DataCitation, CommentsSection, ReportChart...
│       ├── composables/       # 组合式函数
│       ├── layouts/           # 布局组件(含移动端适配)
│       ├── router/            # 路由 (33条)
│       ├── stores/            # Pinia状态
│       ├── utils/             # 工具函数 (export.ts: CSV/JSON/MD/Excel/SQL)
│       └── views/             # 页面视图 (31个)
├── database/
│   └── schema.sql             # 完整数据库Schema (32张表)
├── docker/
│   └── docker-compose.yml     # Docker编排
└── docs/
    ├── API.md                 # 接口文档
    ├── PROJECT_SPEC.md        # 项目规格说明
    └── PROJECT_STRUCTURE.md   # 项目结构目录
```

## 🔑 核心功能模块

### 1. 认证与权限 (Auth + RBAC + Tenant)
- JWT双Token机制 (AccessToken 2h + RefreshToken 7d)
- 基于角色的访问控制
- 多租户数据隔离
- 审计日志追踪
- **个人中心**: 个人信息编辑、修改密码、通知偏好设置

### 2. 数据中心 (DataHub)
- 多数据源接入: MySQL, PostgreSQL, API, Excel, MinIO
- **文件上传**: 支持 Excel/CSV/JSON 拖拽上传
- 数据集管理与列定义
- **数据预览**: 查看数据集前100行数据
- 连接测试与元数据同步
- **数据质量监控** 🆕: 数据源健康检查、新鲜度检测、质量评分、告警

### 3. 配置中心 (Config Center)
- **Schema管理**: 可视化列编辑器(替代JSON文本框)
- KPI DSL表达式引擎 + **AI建议表达式**
- Prompt模板管理 + **AI优化模板**
- 报表模板管理 + **AI生成模板**
- **模板市场** 🆕: 6个行业预置模板(财务/销售/运营/HR/营销/供应链)
- **数据血缘** 🆕: ECharts力导向图、完整数据链路、影响分析
- 所有配置支持版本化管理

### 4. 配置一致性引擎 (Config Consistency Engine) ⭐
- **版本绑定**: schema_v1 → kpi_v1 → workflow_v1 → report_v1
- **依赖图追踪**: schema → metric → workflow → report
- **校验引擎**: 拒绝无效的跨版本引用
- **配置回滚**: 支持回滚到任意版本快照
- **可视化**: SVG依赖图展示

### 5. 工作流引擎 (Workflow Engine)
- DAG定义与**可视化节点编辑器**
- **并行执行**: 按拓扑层级并行执行独立节点
- 状态机: PENDING → RUNNING → SUCCESS/FAILED
- 失败节点单独恢复 (Resume)
- 状态快照与断点恢复
- 节点级执行追踪
- **运行列表自动刷新**: 5秒轮询开关
- **触发参数** 🆕: 支持日期范围、通知邮箱等参数

### 6. 执行可观测性 (Execution Observability) ⭐
- 全链路执行追踪
- **DAG拓扑可视化**: SVG节点状态着色
- 节点级延迟统计
- 错误堆栈捕获
- Token消耗与成本统计
- 执行日志实时查看

### 7. AI控制与安全引擎 (AI Safety Engine) ⭐
- **策略引擎**: 控制AI可访问的数据和操作
- **SQL安全层**: AST解析、注入检测、表/列白名单
- **Prompt防护**: 20+注入模式检测
- **输出校验**: JSON Schema验证
- **执行追踪**: 完整的AI调用链路记录
- **置信度评分** 🆕: NL2SQL和分析结果自动评分(0-100)
- **数据溯源** 🆕: AI输出标注数据来源、字段、时间范围
- **AI免责声明** 🆕: 可复用组件，支持warning/info级别

### 8. 输出引擎 (Output Engine)
- Word (Apache POI)
- PowerPoint (POI XSLF)
- **Excel** 导出支持
- PDF (iText)
- MinIO文件存储
- **报表免责声明注入** 🆕: 自动嵌入免责声明和数据溯源
- **定时报表** 🆕: Cron调度、邮件分发、收件人管理
- **报表版本管理** 🆕: 版本对比、变更追踪、回滚
- **报表分享** 🆕: 生成分享链接、有效期控制

### 9. 通知中心 ⭐
- 站内通知: 工作流完成/失败、报表生成、系统告警
- 通知铃铛: 未读计数、快速标记已读
- 通知列表页: 筛选、批量操作
- 通知偏好设置

### 10. 系统设置 ⭐
- **外观设置**: 浅色/深色/跟随系统三种主题模式、主题色选择
- **AI配置**: 模型、Temperature、安全策略、调用限制
- **通知设置**: 站内/邮件/Webhook 渠道配置
- **安全设置**: 登录超时、双因素认证、IP白名单、密码策略
- **高级设置**: 数据保留策略、并发限制、调试模式

### 11. 智能报警 🆕
- **自然语言规则**: 用中文描述报警条件，AI自动解析
- 示例: "当销售额连续3天下降超过10%时通知我"
- 多通道通知: 站内/邮件/Webhook
- 规则测试、暂停、恢复

### 12. 对话式分析 🆕
- 多轮对话，上下文记忆
- 内嵌图表、SQL、置信度
- 对话历史管理
- 对话导出(Markdown/JSON)

### 13. 数据脱敏与行级权限 🆕
- **数据脱敏**: 手机号/身份证/邮箱/银行卡/姓名/地址自动脱敏
- **行级权限**: 基于角色的行级数据过滤(admin/manager/analyst/viewer)
- **API**: 脱敏API、RLS过滤器、权限检查

## 🖥️ 前端页面

| 页面 | 路径 | 功能 |
|------|------|------|
| 仪表盘 | /dashboard | 统计卡片、最近运行、状态分布图、**AI用量统计** |
| 数据源 | /datahub/datasources | 数据源CRUD + 连接测试 + **文件上传** |
| 数据集 | /datahub/datasets | 数据集CRUD + 列预览 + **数据预览** |
| **数据质量** | /datahub/quality | 🆕 数据源健康检查、质量评分、告警 |
| Schema管理 | /config/schemas | Schema CRUD + **可视化列编辑器** + 版本历史 |
| KPI管理 | /config/kpis | KPI CRUD + DSL编辑器 + 执行 + 版本 + **AI建议** |
| Prompt管理 | /config/prompts | Prompt模板CRUD + 变量提取 + **AI优化** |
| 报表模板 | /config/report-templates | 报表模板CRUD + **AI生成** |
| **模板市场** | /config/template-market | 🆕 行业预置模板、分类筛选、一键使用 |
| **数据血缘** | /config/lineage | 🆕 血缘关系图、影响分析、图表导出 |
| 配置一致性 | /config/consistency | 依赖图、校验、快照、回滚 |
| 工作流 | /workflow/definitions | 工作流CRUD + **可视化DAG编辑器** |
| 运行列表 | /workflow/runs | 运行状态列表 + **自动刷新** |
| 运行详情 | /workflow/run/:id | **DAG拓扑图** + 执行时间线 + 节点详情 + 日志 + 恢复 |
| NL2SQL | /ai/nl2sql | 自然语言转SQL + **置信度** + **数据溯源** + **免责声明** + **导出** |
| AI分析 | /ai/analysis | 数据分析 + ECharts + **置信度** + **数据溯源** + **免责声明** |
| **对话式分析** | /ai/chat | 🆕 多轮对话、上下文记忆、内嵌图表 |
| **智能报警** | /ai/alerts | 🆕 自然语言规则、AI解析、多通道通知 |
| AI追踪 | /ai/traces | AI执行链路追踪 + **复制Prompt** |
| 报表输出 | /output/reports | 报表列表 + 下载 + **数据溯源** + **分享** |
| **定时报表** | /output/scheduled | 🆕 Cron调度、邮件分发、立即执行 |
| **版本管理** | /output/versions | 🆕 版本列表、变更对比、回滚 |
| 审计日志 | /audit | 操作审计 + **数据导出(CSV/JSON/Markdown)** |
| **个人中心** | /profile | 个人信息、修改密码、通知偏好、快捷键说明 |
| **系统设置** | /settings | 外观/AI/通知/安全/高级设置 |
| **通知中心** | /notifications | 通知列表、筛选、批量操作 |

## ⌨️ 键盘快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl+K` | 全局搜索 |
| `Ctrl+D` | 返回仪表盘 |
| `Ctrl+B` | 切换侧边栏 |
| `Ctrl+,` | 打开系统设置 |
| `Ctrl+Shift+D` | 切换深色模式 |

## 🎨 深色模式

支持三种主题模式：
- **浅色模式**: 默认白色背景
- **深色模式**: 暗色背景，减少眼睛疲劳
- **跟随系统**: 自动匹配操作系统设置

深色模式覆盖 30+ Ant Design 组件和所有自定义组件。

## 📊 数据导出

支持以下格式的数据导出：
- **CSV** - 通用表格格式
- **JSON** - 结构化数据
- **Markdown** - 文档格式
- **Excel** - 带格式化的HTML表格(兼容Excel)
- **SQL** - SQL查询文件导出
- **带免责声明导出** - 自动附加AI免责声明

## 🔧 API端点

### Auth
- `POST /api/auth/login` - 登录
- `POST /api/auth/register` - 注册
- `POST /api/auth/refresh` - 刷新Token
- `GET /api/auth/me` - 当前用户信息
- `PUT /api/auth/profile` - 更新个人资料
- `PUT /api/auth/password` - 修改密码
- `PUT /api/auth/notification-preferences` - 通知偏好
- `GET /api/auth/my-stats` - 我的统计

### Dashboard
- `GET /api/dashboard/stats` - 仪表盘统计
- `GET /api/dashboard/recent-runs` - 最近运行
- `GET /api/dashboard/run-status-distribution` - 运行状态分布
- `GET /api/dashboard/ai-stats` - AI用量统计 🆕

### Config Consistency
- `POST /api/config/validate` - 一致性校验
- `GET /api/config/dependency-graph` - 依赖关系图
- `POST /api/config/snapshots` - 创建快照
- `GET /api/config/snapshots` - 快照列表
- `POST /api/config/snapshots/{id}/restore` - 恢复快照
- `GET /api/config/snapshots/diff?id1=1&id2=2` - 快照对比
- `GET /api/config/export` - 导出配置JSON
- `POST /api/config/import` - 导入配置JSON

### Workflow
- `POST /api/workflows/{id}/trigger` - 触发工作流
- `GET /api/workflow-runs` - 运行列表
- `GET /api/workflow-runs/{id}` - 运行详情
- `POST /api/workflow-runs/{id}/resume` - 从失败处恢复

### DataSource
- `POST /api/datasources/upload` - 上传文件(Excel/CSV/JSON)

### AI Service
- `POST /api/ai/nl2sql` - 自然语言转SQL (含置信度+数据溯源+免责声明)
- `POST /api/ai/nl2sql/validate` - SQL验证
- `POST /api/ai/nl2sql/execute` - SQL执行
- `POST /api/ai/analysis` - 数据分析 (含置信度+数据溯源+免责声明)
- `POST /api/ai/report/generate` - 生成报表
- `GET /api/ai/traces` - 执行追踪
- `POST /api/ai/suggest-columns` - AI建议列定义
- `POST /api/ai/suggest-expression` - AI建议KPI表达式
- `POST /api/ai/optimize-prompt` - AI优化提示词
- `POST /api/ai/generate-report-template` - AI生成报表模板

### Data Quality 🆕
- `GET /api/data-quality/overview` - 数据质量概览
- `GET /api/data-quality/health` - 数据源健康状态
- `POST /api/data-quality/check/{id}` - 执行数据源检查

### Alert Rules 🆕
- `GET /api/alert-rules` - 报警规则列表
- `POST /api/alert-rules` - 创建规则
- `PUT /api/alert-rules/{id}` - 更新规则
- `DELETE /api/alert-rules/{id}` - 删除规则
- `POST /api/alert-rules/{id}/test` - 测试规则
- `POST /api/alert-rules/{id}/pause` - 暂停规则
- `POST /api/alert-rules/{id}/resume` - 恢复规则
- `POST /api/alert-rules/parse` - AI解析自然语言规则

### Report Schedule 🆕
- `GET /api/report-schedules` - 定时报表列表
- `POST /api/report-schedules` - 创建定时任务
- `PUT /api/report-schedules/{id}` - 更新定时任务
- `DELETE /api/report-schedules/{id}` - 删除定时任务
- `POST /api/report-schedules/{id}/execute` - 立即执行
- `POST /api/report-schedules/{id}/pause` - 暂停
- `POST /api/report-schedules/{id}/resume` - 恢复

### Data Lineage 🆕
- `GET /api/data-lineage/output/{refType}/{refId}` - 获取输出的数据溯源
- `GET /api/data-lineage/dataset/{datasetId}` - 获取数据集的影响分析
- `GET /api/data-lineage/recent` - 最近溯源记录

### Data Security 🆕
- `POST /api/data-security/mask` - 数据脱敏
- `POST /api/data-security/auto-mask` - 自动检测并脱敏
- `GET /api/data-security/rls-filter` - 获取行级安全过滤器
- `POST /api/data-security/check-access` - 检查数据访问权限

### Share Links 🆕
- `POST /api/share-links` - 创建分享链接
- `GET /api/share-links/{refType}/{refId}` - 访问分享链接

### Notifications
- `GET /api/notifications` - 通知列表
- `GET /api/notifications/unread-count` - 未读数量
- `PUT /api/notifications/{id}/read` - 标记已读
- `PUT /api/notifications/read-all` - 全部已读
- `PUT /api/notifications/clear` - 清空通知
- `DELETE /api/notifications/{id}` - 删除通知

### Settings
- `GET /api/settings` - 获取系统设置
- `PUT /api/settings/appearance` - 外观设置
- `PUT /api/settings/ai` - AI配置
- `PUT /api/settings/notifications` - 通知设置
- `PUT /api/settings/security` - 安全设置
- `PUT /api/settings/advanced` - 高级设置

## 🛡️ 安全特性

1. **JWT双Token**: AccessToken短期有效，RefreshToken长期有效
2. **多租户隔离**: MyBatis Plus拦截器自动注入tenant_id
3. **RBAC权限**: 角色-权限树，API级别权限控制
4. **AI安全策略**: 策略引擎控制AI可访问范围
5. **SQL注入防护**: AST解析 + 关键字检测 + 表/列白名单
6. **Prompt注入防护**: 20+攻击模式检测
7. **审计日志**: 全操作审计追踪
8. **CORS安全**: 环境变量白名单限制
9. **密码加密**: BCrypt + AES加密存储
10. **API限流**: Redis滑动窗口限流，默认100次/分钟/租户
11. **数据脱敏** 🆕: 手机/邮箱/身份证/银行卡自动脱敏
12. **行级权限** 🆕: 基于角色的行级数据过滤

## 📊 数据库

共32张表，覆盖：
- 认证与权限: 6张 (tenant, sys_user, sys_role, sys_permission, sys_user_role, sys_role_permission)
- 数据中心: 3张 (data_source, dataset, dataset_column)
- Schema注册: 1张 (schema_definition)
- KPI引擎: 2张 (kpi_definition, kpi_result)
- 工作流: 5张 (workflow_definition, workflow_run, workflow_node_run, workflow_execution_log, workflow_state_snapshot)
- 配置一致性: 3张 (config_version, config_dependency_graph, config_snapshot)
- 模板: 2张 (prompt_template, report_template)
- AI安全: 3张 (ai_policy, ai_execution_trace, ai_sql_validation_log)
- 输出与审计: 2张 (report_output, audit_log)
- **新增** 🆕: 5张 (data_lineage, report_schedule, report_comment, report_bookmark, share_link)
- **新增** 🆕: 1张 (alert_rule)

## 🔄 开发模式

### 本地开发

```bash
# 后端
cd backend-java
mvn spring-boot:run

# AI服务
cd ai-service-python
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8081

# 前端
cd frontend-vue
npm install
npm run dev
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| LLM_API_KEY | AI模型API Key | - |
| LLM_BASE_URL | AI模型API地址 | https://api.openai.com/v1 |
| LLM_MODEL | AI模型名称 | gpt-4 |
| CORS_ORIGINS | 允许的跨域来源 | http://localhost:3000,http://localhost:8080 |
| MYSQL_PASSWORD | MySQL密码 | report123456 |
| REDIS_PASSWORD | Redis密码 | redis123456 |

## 📝 License

Enterprise License - 仅供企业内部使用
