import { get } from './request'

export interface AuditLog {
  id: string
  userId: string
  username: string
  action: string
  resourceType: string
  resourceId: string
  resourceName: string
  details: Record<string, unknown>
  ipAddress: string
  createdAt: string
}

export const auditApi = {
  list: (params: {
    page: number
    pageSize: number
    action?: string
    resourceType?: string
    userId?: string
    startDate?: string
    endDate?: string
  }) => get<{ data: { items: AuditLog[]; total: number } }>('/audit-logs', params),
}
