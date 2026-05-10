# 项目结构目录

```
enterprise-ai-report/
│
├── README.md                              # 项目说明文档
├── start.sh                               # 一键启动脚本
├── .gitignore                             # Git忽略配置
│
├── docs/                                  # 📚 文档
│   ├── PROJECT_SPEC.md                    #    项目规格说明
│   ├── API.md                             #    接口文档
│   └── PROJECT_STRUCTURE.md               #    项目结构目录(本文件)
│
├── database/                              # 🗄️ 数据库
│   └── schema.sql                         #    完整DDL(27张表+Demo数据)
│
├── docker/                                # 🐳 Docker部署
│   └── docker-compose.yml                 #    服务编排(MySQL+Redis+MinIO+后端+AI+前端)
│
├── backend-java/                          # ☕ Java后端(Spring Boot 3)
│   ├── pom.xml                            #    Maven依赖
│   ├── Dockerfile                         #    Docker镜像
│   └── src/main/
│       ├── resources/
│       │   ├── application.yml            #    主配置
│       │   └── application-docker.yml     #    Docker环境配置
│       └── java/com/enterprise/report/
│           ├── ReportApplication.java     # ⭐ 启动类
│           │
│           ├── config/                    # ⚙️ 配置层
│           │   ├── SecurityConfig.java    #    Spring Security配置
│           │   ├── MybatisPlusConfig.java #    MyBatis Plus配置(租户拦截器)
│           │   ├── RedisConfig.java       #    Redis配置
│           │   └── MinioConfig.java       #    MinIO客户端配置
│           │
│           ├── security/                  # 🔒 安全层
│           │   ├── JwtTokenProvider.java  #    JWT生成/验证
│           │   ├── JwtAuthenticationFilter.java  # JWT过滤器
│           │   ├── UserDetailsImpl.java   #    用户详情
│           │   └── TenantContext.java     #    租户上下文(ThreadLocal)
│           │
│           ├── entity/                    # 📦 实体层(27个)
│           │   ├── Tenant.java            #    租户
│           │   ├── SysUser.java           #    用户
│           │   ├── SysRole.java           #    角色
│           │   ├── SysPermission.java     #    权限
│           │   ├── SysUserRole.java       #    用户角色关联
│           │   ├── SysRolePermission.java #    角色权限关联
│           │   ├── DataSource.java        #    数据源
│           │   ├── Dataset.java           #    数据集
│           │   ├── DatasetColumn.java     #    数据集列
│           │   ├── SchemaDefinition.java  #    Schema定义
│           │   ├── KpiDefinition.java     #    KPI定义
│           │   ├── KpiResult.java         #    KPI结果
│           │   ├── WorkflowDefinition.java#    工作流定义
│           │   ├── WorkflowRun.java       #    工作流运行
│           │   ├── WorkflowNodeRun.java   #    节点运行记录
│           │   ├── WorkflowExecutionLog.java # 执行日志
│           │   ├── WorkflowStateSnapshot.java # 状态快照
│           │   ├── ConfigVersion.java     #    配置版本
│           │   ├── ConfigDependencyGraph.java # 配置依赖图
│           │   ├── ConfigSnapshot.java    #    配置快照
│           │   ├── PromptTemplate.java    #    Prompt模板
│           │   ├── ReportTemplate.java    #    报表模板
│           │   ├── AiPolicy.java          #    AI策略
│           │   ├── AiExecutionTrace.java  #    AI执行追踪
│           │   ├── AiSqlValidationLog.java#    SQL校验日志
│           │   ├── ReportOutput.java      #    报表输出
│           │   └── AuditLog.java          #    审计日志
│           │
│           ├── enums/                     # 📋 枚举
│           │   ├── WorkflowState.java     #    PENDING/RUNNING/SUCCESS/FAILED/RETRYING/PAUSED/CANCELLED
│           │   ├── ConfigType.java        #    schema/kpi/workflow/prompt/report
│           │   ├── DataSourceType.java    #    mysql/postgresql/api/excel/minio
│           │   └── ReportFormat.java      #    word/ppt/pdf
│           │
│           ├── mapper/                    # 🗃️ Mapper层(27个)
│           │   └── *Mapper.java           #    每实体一个Mapper
│           │
│           ├── service/                   # 🔧 服务层(16个接口+16个实现)
│           │   ├── AuthService.java       #    认证服务
│           │   ├── UserService.java       #    用户服务(RBAC)
│           │   ├── TenantService.java     #    租户服务
│           │   ├── DataSourceService.java #    数据源服务
│           │   ├── DatasetService.java    #    数据集服务
│           │   ├── SchemaService.java     #    Schema服务(版本化)
│           │   ├── KpiService.java        #    KPI服务(DSL求值)
│           │   ├── WorkflowService.java   #    工作流服务
│           │   ├── WorkflowExecutionService.java # 工作流执行服务
│           │   ├── ConfigConsistencyService.java # 配置一致性服务
│           │   ├── PromptTemplateService.java    # Prompt模板服务
│           │   ├── ReportTemplateService.java    # 报表模板服务
│           │   ├── AiPolicyService.java   #    AI策略服务
│           │   ├── ReportOutputService.java      # 报表输出服务
│           │   ├── MinioService.java      #    MinIO文件服务
│           │   ├── AuditService.java      #    审计服务
│           │   └── impl/                  #    各服务实现类
│           │
│           ├── controller/                # 🌐 控制器层(15个)
│           │   ├── AuthController.java    #    POST /api/auth/login|register|refresh, GET /me
│           │   ├── TenantController.java  #    CRUD /api/tenants
│           │   ├── UserController.java    #    CRUD /api/users
│           │   ├── DataSourceController.java # CRUD /api/datasources
│           │   ├── DatasetController.java #    CRUD /api/datasets
│           │   ├── SchemaController.java  #    CRUD /api/schemas
│           │   ├── KpiController.java     #    CRUD /api/kpis
│           │   ├── WorkflowController.java#    CRUD /api/workflows
│           │   ├── WorkflowRunController.java # /api/workflow-runs
│           │   ├── ConfigConsistencyController.java # /api/config/*
│           │   ├── PromptTemplateController.java    # /api/prompts
│           │   ├── ReportTemplateController.java    # /api/report-templates
│           │   ├── ReportOutputController.java      # /api/reports
│           │   ├── AiTraceController.java #    /api/ai/traces
│           │   └── AuditLogController.java#    /api/audit-logs
│           │
│           ├── engine/                    # 🚀 核心引擎
│           │   ├── kpi/
│           │   │   └── KpiDslEvaluator.java # KPI DSL求值器
│           │   ├── workflow/
│           │   │   ├── DagExecutor.java   #    DAG执行引擎(拓扑排序+状态机)
│           │   │   ├── NodeExecutor.java  #    节点执行器接口
│           │   │   ├── KpiCalcNodeExecutor.java    # KPI计算节点
│           │   │   ├── AiAnalysisNodeExecutor.java # AI分析节点
│           │   │   └── OutputNodeExecutor.java     # 报表输出节点
│           │   ├── consistency/
│           │   │   ├── DependencyValidator.java # 依赖校验器
│           │   │   └── VersionManager.java      # 版本管理器
│           │   └── output/
│           │       └── ReportGenerator.java     # 报表生成(Word/PPT/PDF)
│           │
│           ├── dto/                       # 📨 数据传输对象
│           │   ├── ApiResponse.java       #    统一响应
│           │   ├── PageResult.java        #    分页结果
│           │   ├── auth/                  #    认证相关DTO
│           │   ├── dataset/               #    数据集相关DTO
│           │   ├── schema/                #    Schema相关DTO
│           │   ├── kpi/                   #    KPI相关DTO
│           │   ├── workflow/              #    工作流相关DTO
│           │   └── config/                #    配置一致性DTO
│           │
│           ├── exception/                 # ⚠️ 异常处理
│           │   ├── BusinessException.java #    业务异常
│           │   └── GlobalExceptionHandler.java # 全局异常处理
│           │
│           └── util/                      # 🛠️ 工具类
│               └── EncryptionUtil.java    #    AES加密工具
│
├── ai-service-python/                     # 🐍 Python AI服务(FastAPI)
│   ├── requirements.txt                   #    Python依赖
│   ├── Dockerfile                         #    Docker镜像
│   ├── .env.example                       #    环境变量模板
│   ├── alembic.ini                        #    数据库迁移配置
│   ├── alembic/
│   │   ├── env.py                         #    迁移环境
│   │   └── script.py.mako                 #    迁移脚本模板
│   └── app/
│       ├── main.py                        # ⭐ FastAPI入口
│       ├── core/                          # ⚙️ 核心配置
│       │   ├── config.py                  #    Pydantic Settings
│       │   ├── database.py                #    异步SQLAlchemy引擎
│       │   ├── redis.py                   #    Redis客户端
│       │   ├── minio_client.py            #    MinIO客户端
│       │   └── dependencies.py            #    FastAPI依赖注入
│       ├── models/                        # 📦 ORM模型(14个)
│       │   ├── base.py                    #    基础模型(UUID主键+时间戳)
│       │   ├── tenant.py, user.py, data_source.py, dataset.py
│       │   ├── schema_definition.py, kpi_definition.py
│       │   ├── workflow.py, workflow_run.py
│       │   ├── ai_policy.py, ai_trace.py
│       │   └── prompt_template.py, report_template.py
│       ├── schemas/                       # 📋 Pydantic Schema
│       │   ├── common.py                  #    ApiResponse, PageResult
│       │   ├── auth.py                    #    Token相关
│       │   ├── ai.py                      #    AI请求/响应
│       │   └── trace.py                   #    追踪相关
│       ├── services/                      # 🔧 业务服务(10个)
│       │   ├── auth_service.py            #    JWT验证
│       │   ├── ai_policy_service.py       #    策略执行
│       │   ├── nl2sql_service.py          #    NL2SQL编排
│       │   ├── analysis_service.py        #    分析编排
│       │   ├── report_service.py          #    报表生成编排
│       │   ├── sql_validator.py           #    SQL安全验证
│       │   ├── prompt_builder.py          #    Prompt构建器
│       │   ├── cost_tracker.py            #    成本追踪
│       │   └── trace_service.py           #    追踪服务
│       ├── engines/                       # 🚀 LangGraph引擎
│       │   ├── nl2sql/                    #    NL2SQL工作流
│       │   │   ├── graph.py               #      图定义
│       │   │   ├── nodes.py               #      节点实现
│       │   │   └── state.py               #      状态定义
│       │   ├── analysis/                  #    数据分析工作流
│       │   │   ├── graph.py, nodes.py, state.py
│       │   └── report/                    #    报表生成工作流
│       │       ├── graph.py, nodes.py, state.py
│       ├── safety/                        # 🛡️ AI安全层
│       │   ├── sql_validator.py           #    SQL AST解析+注入检测
│       │   ├── prompt_guard.py            #    Prompt注入防护(20+模式)
│       │   ├── output_validator.py        #    输出JSON Schema校验
│       │   └── policy_engine.py           #    策略执行引擎
│       ├── prompts/                       # 📝 Prompt模板
│       │   ├── nl2sql_system.py           #    NL2SQL系统提示词
│       │   ├── nl2sql_user.py             #    NL2SQL用户提示词
│       │   ├── analysis_system.py         #    分析系统提示词
│       │   ├── analysis_user.py           #    分析用户提示词
│       │   ├── report_system.py           #    报表系统提示词
│       │   └── report_user.py             #    报表用户提示词
│       └── api/                           # 🌐 API路由
│           ├── router.py                  #    路由汇总
│           ├── auth.py                    #    /api/auth/*
│           ├── nl2sql.py                  #    /api/ai/nl2sql
│           ├── analysis.py                #    /api/ai/analysis
│           ├── report.py                  #    /api/ai/report/*
│           ├── traces.py                  #    /api/ai/traces
│           └── health.py                  #    /api/health
│
├── frontend-vue/                          # 🖥️ Vue 3 前端
│   ├── package.json                       #    NPM依赖
│   ├── vite.config.ts                     #    Vite配置
│   ├── tsconfig.json                      #    TypeScript配置
│   ├── index.html                         #    HTML入口
│   ├── Dockerfile                         #    Docker镜像(多阶段)
│   ├── nginx.conf                         #    Nginx配置(SPA+API代理)
│   ├── .env.development                   #    开发环境变量
│   ├── .env.production                    #    生产环境变量
│   └── src/
│       ├── main.ts                        # ⭐ Vue入口
│       ├── App.vue                        #    根组件
│       ├── env.d.ts                       #    TS声明
│       ├── api/                           # 🌐 API模块(14个)
│       │   ├── request.ts                 #    Axios实例(JWT拦截器)
│       │   ├── auth.ts                    #    认证API
│       │   ├── tenant.ts                  #    租户API
│       │   ├── user.ts                    #    用户API
│       │   ├── datasource.ts              #    数据源API
│       │   ├── dataset.ts                 #    数据集API
│       │   ├── schema.ts                  #    Schema API
│       │   ├── kpi.ts                     #    KPI API
│       │   ├── workflow.ts                #    工作流API
│       │   ├── prompt.ts                  #    Prompt API
│       │   ├── report-template.ts         #    报表模板API
│       │   ├── report-output.ts           #    报表输出API
│       │   ├── config.ts                  #    配置一致性API
│       │   ├── ai-trace.ts                #    AI追踪API
│       │   └── audit.ts                   #    审计日志API
│       ├── router/                        # 🛤️ 路由
│       │   ├── index.ts                   #    路由定义
│       │   └── guards.ts                  #    路由守卫
│       ├── stores/                        # 📊 Pinia状态
│       │   ├── index.ts                   #    Pinia配置
│       │   ├── auth.ts                    #    认证状态
│       │   ├── app.ts                     #    应用状态
│       │   └── tenant.ts                  #    租户状态
│       ├── layouts/                       # 📐 布局
│       │   ├── MainLayout.vue             #    主布局(侧栏+顶栏)
│       │   └── BasicLayout.vue            #    基础布局(登录页)
│       ├── components/                    # 🧩 公共组件
│       │   └── common/
│       │       ├── PageHeader.vue         #    页头+面包屑
│       │       ├── SearchForm.vue         #    搜索表单
│       │       └── ConfirmDelete.vue      #    删除确认
│       ├── composables/                   # 🪝 组合式函数
│       │   ├── useTable.ts                #    表格逻辑复用
│       │   ├── useModal.ts                #    弹窗逻辑复用
│       │   └── usePermission.ts           #    权限检查
│       ├── assets/styles/                 # 🎨 样式
│       │   ├── variables.scss             #    SCSS变量
│       │   └── main.scss                  #    全局样式
│       └── views/                         # 📄 页面(22个)
│           ├── auth/
│           │   └── LoginView.vue          #    登录页
│           ├── dashboard/
│           │   └── DashboardView.vue      #    仪表盘(统计+图表)
│           ├── admin/
│           │   ├── TenantView.vue         #    租户管理
│           │   ├── UserView.vue           #    用户管理
│           │   └── RoleView.vue           #    角色管理
│           ├── datahub/
│           │   ├── DataSourceView.vue     #    数据源管理
│           │   └── DatasetView.vue        #    数据集管理
│           ├── config/
│           │   ├── SchemaView.vue         #    Schema管理(JSON编辑+版本)
│           │   ├── KpiView.vue            #    KPI管理(DSL编辑+执行)
│           │   ├── PromptView.vue         #    Prompt管理
│           │   ├── ReportTemplateView.vue #    报表模板管理
│           │   └── ConsistencyView.vue    #    配置一致性(SVG依赖图+校验+快照)
│           ├── workflow/
│           │   ├── WorkflowView.vue       #    工作流定义(DAG编辑)
│           │   ├── WorkflowRunListView.vue#    运行列表
│           │   └── WorkflowRunDetailView.vue # 运行详情(时间线+节点+日志)
│           ├── ai/
│           │   ├── Nl2sqlView.vue         #    NL2SQL Playground
│           │   ├── AnalysisView.vue       #    AI分析(ECharts可视化)
│           │   ├── TraceListView.vue      #    追踪列表
│           │   └── TraceDetailView.vue    #    追踪详情(Prompt+输出+成本)
│           ├── output/
│           │   └── ReportView.vue         #    报表输出(下载)
│           └── audit/
│               └── AuditLogView.vue       #    审计日志
│
└── tests/                                 # 🧪 测试
    └── ai-service-python/
        ├── conftest.py                    #    测试配置
        ├── test_sql_validator.py          #    SQL校验测试(17项)
        ├── test_nl2sql.py                 #    NL2SQL测试
        └── test_policy_engine.py          #    策略引擎测试(18项)
```

## 统计摘要

| 模块 | 文件数 | 代码行数 | 技术栈 |
|------|--------|----------|--------|
| Java后端 | 148 | 5,859 | Spring Boot 3.2 + MyBatis Plus |
| Python AI服务 | 76 | 5,158 | FastAPI + LangGraph |
| Vue前端 | 54 | 6,681 | Vue 3 + Ant Design Vue |
| 数据库 | 1 | 556 | MySQL 8 |
| Docker | 1 | 85 | Docker Compose |
| 文档 | 4 | ~800 | Markdown |
| **合计** | **~303** | **~18,254** | - |
