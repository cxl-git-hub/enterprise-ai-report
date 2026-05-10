import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { authGuard } from './guards'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '仪表盘', icon: 'DashboardOutlined' },
      },
      {
        path: 'admin/tenants',
        name: 'Tenants',
        component: () => import('@/views/admin/TenantView.vue'),
        meta: { title: '租户管理', icon: 'BankOutlined', permission: 'tenant:manage' },
      },
      {
        path: 'admin/users',
        name: 'Users',
        component: () => import('@/views/admin/UserView.vue'),
        meta: { title: '用户管理', icon: 'UserOutlined', permission: 'user:manage' },
      },
      {
        path: 'admin/roles',
        name: 'Roles',
        component: () => import('@/views/admin/RoleView.vue'),
        meta: { title: '角色管理', icon: 'SafetyOutlined', permission: 'role:manage' },
      },
      {
        path: 'datahub/datasources',
        name: 'DataSources',
        component: () => import('@/views/datahub/DataSourceView.vue'),
        meta: { title: '数据源管理', icon: 'DatabaseOutlined', permission: 'datasource:read' },
      },
      {
        path: 'datahub/datasets',
        name: 'Datasets',
        component: () => import('@/views/datahub/DatasetView.vue'),
        meta: { title: '数据集管理', icon: 'TableOutlined', permission: 'dataset:read' },
      },
      {
        path: 'config/schemas',
        name: 'Schemas',
        component: () => import('@/views/config/SchemaView.vue'),
        meta: { title: 'Schema管理', icon: 'NodeIndexOutlined', permission: 'schema:read' },
      },
      {
        path: 'config/kpis',
        name: 'Kpis',
        component: () => import('@/views/config/KpiView.vue'),
        meta: { title: 'KPI管理', icon: 'FundOutlined', permission: 'kpi:read' },
      },
      {
        path: 'config/prompts',
        name: 'Prompts',
        component: () => import('@/views/config/PromptView.vue'),
        meta: { title: '提示词模板', icon: 'FileTextOutlined', permission: 'prompt:read' },
      },
      {
        path: 'config/report-templates',
        name: 'ReportTemplates',
        component: () => import('@/views/config/ReportTemplateView.vue'),
        meta: { title: '报表模板', icon: 'FileSearchOutlined', permission: 'report-template:read' },
      },
      {
        path: 'config/consistency',
        name: 'Consistency',
        component: () => import('@/views/config/ConsistencyView.vue'),
        meta: { title: '配置一致性', icon: 'CheckCircleOutlined', permission: 'config:manage' },
      },
      {
        path: 'workflow/definitions',
        name: 'WorkflowDefinitions',
        component: () => import('@/views/workflow/WorkflowView.vue'),
        meta: { title: '工作流定义', icon: 'ApartmentOutlined', permission: 'workflow:read' },
      },
      {
        path: 'workflow/runs',
        name: 'WorkflowRuns',
        component: () => import('@/views/workflow/WorkflowRunListView.vue'),
        meta: { title: '工作流运行', icon: 'PlayCircleOutlined', permission: 'workflow:read' },
      },
      {
        path: 'workflow/run/:runId',
        name: 'WorkflowRunDetail',
        component: () => import('@/views/workflow/WorkflowRunDetailView.vue'),
        meta: { title: '运行详情', hidden: true, permission: 'workflow:read' },
      },
      {
        path: 'ai/nl2sql',
        name: 'Nl2sql',
        component: () => import('@/views/ai/Nl2sqlView.vue'),
        meta: { title: 'NL2SQL', icon: 'ThunderboltOutlined', permission: 'ai:nl2sql' },
      },
      {
        path: 'ai/analysis',
        name: 'Analysis',
        component: () => import('@/views/ai/AnalysisView.vue'),
        meta: { title: 'AI分析', icon: 'BarChartOutlined', permission: 'ai:analysis' },
      },
      {
        path: 'ai/traces',
        name: 'AiTraces',
        component: () => import('@/views/ai/TraceListView.vue'),
        meta: { title: 'AI执行追踪', icon: 'EyeOutlined', permission: 'ai:trace' },
      },
      {
        path: 'ai/traces/:traceId',
        name: 'AiTraceDetail',
        component: () => import('@/views/ai/TraceDetailView.vue'),
        meta: { title: '追踪详情', hidden: true, permission: 'ai:trace' },
      },
      {
        path: 'output/reports',
        name: 'Reports',
        component: () => import('@/views/output/ReportView.vue'),
        meta: { title: '报表输出', icon: 'DownloadOutlined', permission: 'report:read' },
      },
      {
        path: 'audit',
        name: 'AuditLogs',
        component: () => import('@/views/audit/AuditLogView.vue'),
        meta: { title: '审计日志', icon: 'AuditOutlined', permission: 'audit:read' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心', icon: 'UserOutlined', hidden: true },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { title: '系统设置', icon: 'SettingOutlined', hidden: true },
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/notifications/NotificationView.vue'),
        meta: { title: '通知中心', icon: 'BellOutlined', hidden: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(authGuard)

export default router
