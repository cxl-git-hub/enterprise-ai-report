import { get } from './request'

export interface AuditLog {
  id: string
  userId: string
  username: string
  action: string
  resourceType: string
  resourceId: string
  resourceName: string
  details: Record<string, unknown> | string
  ipAddress: string
  createdAt: string
}

/** Map backend audit log to frontend format */
function mapAuditFromBackend(a: any): AuditLog {
  let details: Record<string, unknown> | string = a.details || ''
  if (typeof details === 'string' && details.startsWith('{')) {
    try {
      details = JSON.parse(details)
    } catch { /* keep as string */ }
  }
  return {
    id: a.id,
    userId: a.userId || '',
    username: a.username || '',
    action: a.action || '',
    resourceType: a.resourceType || '',
    resourceId: a.resourceId || '',
    resourceName: a.resourceName || a.resourceId || '',
    details,
    ipAddress: a.ipAddress || '',
    createdAt: a.createdAt,
  }
}

export const auditApi = {
  list: async (params: {
    page: number
    pageSize: number
    action?: string
    resourceType?: string
    userId?: string
    startDate?: string
    endDate?: string
  }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/audit-logs', params as Record<string, unknown>)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapAuditFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
}
