import { get } from './request'

export interface DashboardStats {
  datasourceCount: number
  kpiCount: number
  workflowCount: number
  reportCount: number
}

export interface RecentRun {
  id: string
  workflowId: string
  workflowName: string
  status: string
  startedAt: string
  duration: number
}

export interface StatusDistribution {
  status: string
  count: number
}

export const dashboardApi = {
  stats: () => get<{ data: DashboardStats }>('/dashboard/stats'),
  recentRuns: (limit?: number) => get<{ data: RecentRun[] }>('/dashboard/recent-runs', { limit: limit || 5 }),
  runStatusDistribution: () => get<{ data: StatusDistribution[] }>('/dashboard/run-status-distribution'),
}
