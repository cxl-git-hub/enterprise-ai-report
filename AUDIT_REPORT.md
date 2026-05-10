# Enterprise AI Report Platform — 商业级审查报告

## 审查概述
从产品、用户体验、技术实现三个维度，对项目的22个前端页面、15个后端控制器、3个AI引擎进行全面审查。

---

## 🔴 严重问题 (P0 - 必须修复)

### 1. Dashboard 仪表盘：100% 硬编码假数据
**文件**: `frontend-vue/src/views/dashboard/DashboardView.vue`
**问题**: 所有统计数据（数据源12个、KPI 45个等）和最近运行记录全部是 `onMounted` 中写死的假数据，未调用任何后端API。这是用户进入系统看到的第一个页面。
**影响**: 商业产品展示的是假数据，用户无法看到真实系统状态。

### 2. DAG 编辑器：原始 JSON 文本框
**文件**: `frontend-vue/src/views/workflow/WorkflowView.vue`
**问题**: 工作流的 DAG 节点定义只提供一个 `a-textarea` 让用户手写 JSON。对于复杂工作流（10+ 节点），这是灾难性的用户体验。
**影响**: 用户无法直观地创建和编辑工作流，容易出错。

### 3. AI 建议功能：全部是空壳
**文件**: `SchemaView.vue`, `KpiView.vue`, `PromptView.vue`, `ReportTemplateView.vue`
**问题**: 4个页面都有"AI建议"按钮，但全部只显示 `message.info('AI建议功能需要连接后端AI服务')`，没有实际调用。
**影响**: 核心卖点功能不可用。

### 4. Excel/文件上传入口缺失
**文件**: `frontend-vue/src/views/datahub/DataSourceView.vue`
**问题**: README 和 prompt 声称支持 Excel/MinIO 数据源，但 DataSourceView 只有数据库连接表单，没有文件上传功能。
**影响**: 数据接入方式不完整。

### 5. Excel 报表导出缺失
**文件**: `frontend-vue/src/views/output/ReportView.vue`
**问题**: Output Engine 声称支持 Word/PPT/PDF，但前端格式筛选有 Excel 选项，后端 ReportGenerator 却没有 Excel 生成逻辑。
**影响**: 用户期望的功能不存在。

### 6. 工作流执行 DAG 可视化缺失
**文件**: `frontend-vue/src/views/workflow/WorkflowRunDetailView.vue`
**问题**: "执行时间线"是线性列表，不是 DAG 拓扑图。用户无法直观看到节点间的依赖关系和并行执行情况。

### 7. 数据源密码明文传输
**文件**: `frontend-vue/src/views/datahub/DataSourceView.vue`
**问题**: 数据库密码以明文形式在表单中显示和传输，没有加密处理。
**影响**: 安全隐患。

---

## 🟡 中等问题 (P1 - 应该修复)

### 8. Schema/KPI 列定义：JSON 文本框不友好
**问题**: Schema 的列定义和 KPI 的 DSL 表达式都用原始 JSON 文本框，非技术用户无法使用。
**建议**: 提供结构化表单 + 可视化编辑器。

### 9. 登录页缺少"记住我"和租户选择
**问题**: 多租户 SaaS 系统登录页没有租户选择器，也没有"记住我"功能。

### 10. 角色权限分配：扁平复选框
**问题**: 权限是树形结构（menu/button/api），但分配时用扁平复选框，无法体现层级关系。

### 11. Prompt/ReportTemplate 编辑器：纯文本
**问题**: 提示词模板和报表模板都用普通 textarea，没有语法高亮、变量提示、自动补全。

### 12. 依赖图 SVG 布局过于简单
**问题**: ConsistencyView 的依赖图用固定分层布局，节点多时会重叠，无法拖拽交互。

### 13. NL2SQL 缺少高级 SQL 编辑器
**问题**: 生成的 SQL 只用 `<pre>` 标签展示，没有语法高亮、行号、错误定位。

### 14. 审计日志日期筛选未同步后端
**问题**: `dateRange` 变化时设置了 `searchParams.startDate/endDate`，但没有触发搜索。

### 15. 后端 CORS 配置 `allow_origins=["*"]`
**文件**: `ai-service-python/app/main.py`
**问题**: 生产环境不应允许所有来源的跨域请求。

### 16. DAG 执行器串行执行
**文件**: `backend-java/.../engine/workflow/DagExecutor.java`
**问题**: 拓扑排序后顺序执行所有节点，没有利用 DAG 的并行能力。独立节点应该可以并行执行。

### 17. AI 分析结果无法导出
**问题**: AnalysisView 的分析结果（图表、发现、叙述）无法导出为 PDF/Excel。

### 18. 工作流节点类型硬编码
**问题**: 前端工作流表单的节点类型没有从后端获取，也不支持自定义节点类型。

### 19. 全局搜索缺失
**问题**: 系统有 22 个页面，但没有全局搜索功能，用户需要逐页查找。

### 20. 响应式布局不完整
**问题**: 多个页面使用固定宽度布局（如 `a-col :span="6"`），在小屏幕上会挤压变形。

---

## 🟢 改进建议 (P2 - 锦上添花)

### 21. 深色模式支持
### 22. 国际化 (i18n) 支持
### 23. 键盘快捷键
### 24. 数据导出（表格导出 CSV/Excel）
### 25. 通知中心（工作流完成、异常告警）
### 26. 用户偏好设置持久化
### 27. 移动端适配

---

## 修复优先级

| 优先级 | 编号 | 预计工作量 |
|--------|------|-----------|
| P0 | 1,2,3,4,5,6,7 | 大 |
| P1 | 8,9,10,11,12,13,14,15,16,17,18,19,20 | 中 |
| P2 | 21-27 | 小 |
