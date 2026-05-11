# Enterprise AI Automated Reporting Platform - Project Specification

> 最后更新: 2026-05-11

## System Overview
Turn enterprise data → KPI system → AI analysis → workflow execution → automated reports.

## Tech Stack
- **Backend**: Java 17, Spring Boot 3, Spring Security + JWT, MyBatis Plus, Redis, MySQL
- **AI Service**: Python 3.10+, FastAPI, LangGraph
- **Frontend**: Vue 3, Vite, Pinia, Ant Design Vue
- **Storage**: MySQL, Redis, MinIO
- **Deploy**: Docker Compose

## Core Modules
1. Auth + RBAC + Tenant System (含个人中心)
2. DataHub (Excel/DB/API/文件上传 Ingestion + 数据质量监控)
3. Config Center (Schema/KPI/Workflow/Prompt/Template + AI建议 + 模板市场 + 数据血缘)
4. KPI Engine (DSL-based)
5. Workflow Engine (DAG Execution + 并行执行)
6. AI Service (NL2SQL + Analysis + Report + Suggestions + 置信度 + 数据溯源 + 对话式分析)
7. Output Engine (Word/PPT/PDF/Excel + 定时报表 + 版本管理 + 分享)
8. Frontend Admin System (31个页面)
9. Notification Center (通知中心)
10. System Settings (系统设置)
11. Alert Rules (智能报警 - 自然语言规则)
12. Data Security (数据脱敏 + 行级权限)

## Commercial Extension Layers
1. **Config Consistency Engine** - Version binding, dependency graph, validation, rollback
2. **Execution State & Observability Engine** - State machine, persistence, recovery, tracing, cost tracking, DAG visualization
3. **AI Control & Safety Engine** - Policy engine, SQL safety, prompt pipeline, output validation, confidence scoring, data citation
4. **Data Quality Engine** - Health checks, freshness monitoring, quality scoring, alerts
5. **Data Security Engine** - Data masking (phone/ID/email/bank), row-level security (admin/manager/analyst/viewer)
6. **Report Scheduling Engine** - Cron scheduling, email distribution, disclaimer injection

## UI/UX Features
- **Deep Dark Mode**: Full dark mode with 30+ component coverage
- **Responsive Layout**: xs/sm/md breakpoints for all pages, mobile sidebar overlay
- **Keyboard Shortcuts**: Ctrl+K search, Ctrl+D dashboard, Ctrl+B sidebar, Ctrl+, settings
- **Data Export**: CSV/JSON/Markdown/Excel/SQL export with AI disclaimer option
- **Global Search**: Header search bar for quick navigation
- **Notification Bell**: Unread count, quick mark read, link to details
- **Back to Top**: Floating button for long pages
- **Chinese Localization**: Ant Design zhCN locale
- **AI Disclaimer**: Reusable component with confidence score, data citations
- **Data Citation**: Source tracking for all AI-generated outputs

## Pages (26 total)
| Page | Path | Key Features |
|------|------|-------------|
| Login | /login | Tenant selector, Remember me |
| Dashboard | /dashboard | Real API stats, Status distribution chart |
| Tenants | /admin/tenants | CRUD |
| Users | /admin/users | CRUD + Role assignment |
| Roles | /admin/roles | CRUD + Permission assignment |
| Data Sources | /datahub/datasources | CRUD + Connection test + File upload |
| Datasets | /datahub/datasets | CRUD + Column preview + Data preview |
| Schemas | /config/schemas | CRUD + Visual column editor + AI suggest + Version history |
| KPIs | /config/kpis | CRUD + DSL editor + Execute + AI suggest + Version history |
| Prompts | /config/prompts | CRUD + Variable extraction + AI optimize |
| Report Templates | /config/report-templates | CRUD + AI generate |
| Consistency | /config/consistency | Dependency graph + Validation + Snapshots + Rollback |
| Workflows | /workflow/definitions | CRUD + Visual DAG editor |
| Run List | /workflow/runs | Status list + Auto-refresh toggle |
| Run Detail | /workflow/run/:id | DAG topology + Timeline + Node detail + Logs + Resume |
| NL2SQL | /ai/nl2sql | Schema context + Generate + Validate + Execute + Export |
| AI Analysis | /ai/analysis | Dataset select + Analysis type + ECharts + Export |
| AI Traces | /ai/traces | Filter by type/status |
| Trace Detail | /ai/traces/:traceId | Prompt copy + Output compare + Cost chart + Retry history |
| Reports | /output/reports | List + Download + Format filter + Data lineage + Share |
| **Scheduled Reports** | /output/scheduled | 🆕 Cron scheduling + Email distribution + Execute now |
| **Report Versions** | /output/versions | 🆕 Version list + Diff compare + Rollback + Download |
| Audit Log | /audit | Filter + Date range + Export (CSV/JSON/Markdown) |
| Profile | /profile | Edit profile + Change password + Notification prefs + Shortcuts |
| Settings | /settings | Appearance + AI + Notification + Security + Advanced |
| Notifications | /notifications | List + Filter + Mark read + Clear |
| **Data Quality** | /datahub/quality | 🆕 Health checks + Quality scoring + Alerts |
| **Template Market** | /config/template-market | 🆕 Industry presets + Category filter + One-click use |
| **Data Lineage** | /config/lineage | 🆕 Lineage graph + Impact analysis + Export |
| **Chat Analysis** | /ai/chat | 🆕 Multi-turn conversation + Context memory + Inline charts |
| **Alert Rules** | /ai/alerts | 🆕 NL rules + AI parse + Multi-channel notify |

## API Endpoints (28 groups)
1. Auth (login/register/refresh/me/profile/password/stats)
2. Tenant CRUD
3. User CRUD + Role assignment
4. DataSource CRUD + Test + Upload
5. Dataset CRUD + Columns + Preview
6. Schema CRUD + Versions
7. KPI CRUD + Execute + Versions
8. Workflow CRUD + Trigger
9. WorkflowRun List + Detail + Resume
10. Config Consistency (validate/graph/snapshots)
11. Prompt CRUD
12. ReportTemplate CRUD
13. ReportOutput List + Download
14. AI NL2SQL + Validate + Execute (with confidence + citations + disclaimer)
15. AI Analysis (with confidence + citations + disclaimer)
16. AI Report Generate
17. AI Traces
18. AI Suggestions (columns/expression/prompt/template)
19. Dashboard (stats/recent-runs/distribution/ai-stats)
20. Notifications (list/read/read-all/clear/delete)
21. Settings (get/appearance/ai/notifications/security/advanced/clear-cache)
22. Data Quality (overview/health/check)
23. Alert Rules (CRUD/test/pause/resume/parse)
24. Report Schedules (CRUD/execute/pause/resume)
25. Data Lineage (output/dataset/recent)
26. Data Security (mask/auto-mask/rls-filter/check-access)
27. Share Links (create/access)
28. File Upload (datasource upload)
