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
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │ gRPC/HTTP
┌────────────────────────┴────────────────────────────────────┐
│              AI Service (FastAPI + LangGraph)                 │
│  NL2SQL │ Analysis │ Report Gen │ Safety Engine │ Policy     │
│  Suggestions (Schema/KPI/Prompt/Report Template AI)          │
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
│       ├── controller/        # REST控制器 (18个)
│       ├── dto/               # 数据传输对象
│       ├── engine/            # 核心引擎
│       │   ├── consistency/   # 配置一致性引擎
│       │   ├── kpi/           # KPI DSL求值器
│       │   ├── output/        # 报表生成器
│       │   └── workflow/      # DAG工作流引擎(支持并行执行)
│       ├── entity/            # 实体类 (27个)
│       ├── enums/             # 枚举
│       ├── exception/         # 异常处理
│       ├── mapper/            # MyBatis Mapper (27个)
│       ├── security/          # JWT安全层
│       ├── service/           # 业务服务 (16个)
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
│       ├── services/          # 业务服务
│       └── utils/             # 工具函数(导出等)
├── frontend-vue/              # Vue 3 前端
│   ├── package.json
│   ├── Dockerfile
│   └── src/
│       ├── api/               # API模块 (14个)
│       ├── components/        # 公共组件 (含 NotificationCenter)
│       ├── composables/       # 组合式函数 (含 useKeyboardShortcuts)
│       ├── layouts/           # 布局组件
│       ├── router/            # 路由
│       ├── stores/            # Pinia状态
│       ├── utils/             # 工具函数 (export.ts)
│       └── views/             # 页面视图 (26个)
├── database/
│   └── schema.sql             # 完整数据库Schema (27张表)
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

### 3. 配置中心 (Config Center)
- **Schema管理**: 可视化列编辑器(替代JSON文本框)
- KPI DSL表达式引擎 + **AI建议表达式**
- Prompt模板管理 + **AI优化模板**
- 报表模板管理 + **AI生成模板**
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

### 8. 输出引擎 (Output Engine)
- Word (Apache POI)
- PowerPoint (POI XSLF)
- **Excel** 导出支持
- PDF (iText)
- MinIO文件存储

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

## 🖥️ 前端页面

| 页面 | 路径 | 功能 |
|------|------|------|
| 仪表盘 | /dashboard | 统计卡片、最近运行、状态分布图 |
| 数据源 | /datahub/datasources | 数据源CRUD + 连接测试 + **文件上传** |
| 数据集 | /datahub/datasets | 数据集CRUD + 列预览 + **数据预览** |
| Schema管理 | /config/schemas | Schema CRUD + **可视化列编辑器** + 版本历史 |
| KPI管理 | /config/kpis | KPI CRUD + DSL编辑器 + 执行 + 版本 + **AI建议** |
| Prompt管理 | /config/prompts | Prompt模板CRUD + 变量提取 + **AI优化** |
| 报表模板 | /config/report-templates | 报表模板CRUD + **AI生成** |
| 配置一致性 | /config/consistency | 依赖图、校验、快照、回滚 |
| 工作流 | /workflow/definitions | 工作流CRUD + **可视化DAG编辑器** |
| 运行列表 | /workflow/runs | 运行状态列表 + **自动刷新** |
| 运行详情 | /workflow/run/:id | **DAG拓扑图** + 执行时间线 + 节点详情 + 日志 + 恢复 |
| NL2SQL | /ai/nl2sql | 自然语言转SQL Playground + **结果导出** |
| AI分析 | /ai/analysis | 数据分析 + ECharts可视化 + **结果导出** |
| AI追踪 | /ai/traces | AI执行链路追踪 + **复制Prompt** |
| 报表输出 | /output/reports | 报表列表 + 下载 |
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

支持以下模块的数据导出：
- **审计日志**: CSV / JSON / Markdown
- **NL2SQL结果**: CSV / JSON
- **AI分析结果**: 数据CSV / 报告Markdown / 完整JSON

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
- `POST /api/ai/nl2sql` - 自然语言转SQL
- `POST /api/ai/analysis` - 数据分析
- `POST /api/ai/report/generate` - 生成报表
- `GET /api/ai/traces` - 执行追踪
- `POST /api/ai/suggest-columns` - AI建议列定义
- `POST /api/ai/suggest-expression` - AI建议KPI表达式
- `POST /api/ai/optimize-prompt` - AI优化提示词
- `POST /api/ai/generate-report-template` - AI生成报表模板

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
8. **CORS安全**: 白名单限制(非 `allow_origins=["*"]`)
9. **密码加密**: BCrypt + AES加密存储
10. **API限流**: Redis滑动窗口限流，默认100次/分钟/租户
11. **数据脱敏**: 手机/邮箱/身份证/银行卡自动脱敏

## 📊 数据库

共27张表，覆盖：
- 认证与权限: 6张 (tenant, sys_user, sys_role, sys_permission, sys_user_role, sys_role_permission)
- 数据中心: 3张 (data_source, dataset, dataset_column)
- Schema注册: 1张 (schema_definition)
- KPI引擎: 2张 (kpi_definition, kpi_result)
- 工作流: 5张 (workflow_definition, workflow_run, workflow_node_run, workflow_execution_log, workflow_state_snapshot)
- 配置一致性: 3张 (config_version, config_dependency_graph, config_snapshot)
- 模板: 2张 (prompt_template, report_template)
- AI安全: 3张 (ai_policy, ai_execution_trace, ai_sql_validation_log)
- 输出与审计: 2张 (report_output, audit_log)

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
| MYSQL_PASSWORD | MySQL密码 | report123456 |
| REDIS_PASSWORD | Redis密码 | redis123456 |

## 📝 License

Enterprise License - 仅供企业内部使用
