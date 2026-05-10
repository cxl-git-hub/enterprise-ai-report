import { get, post, put, del } from './request'

export interface Tenant {
  id: string
  tenantCode: string
  tenantName: string
  contactName: string
  contactEmail: string
  contactPhone: string
  planType: string
  maxUsers: number
  maxDatasets: number
  maxAiCallsPerDay: number
  status: number
  expireTime: string
  createdAt: string
  updatedAt: string
}

export interface TenantForm {
  tenantCode: string
  tenantName: string
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  planType?: string
  maxUsers?: number
  maxDatasets?: number
  maxAiCallsPerDay?: number
}

/** Map backend entity fields (code/name) to frontend fields (tenantCode/tenantName) */
function mapTenantFromBackend(t: any): Tenant {
  return {
    id: t.id,
    tenantCode: t.tenantCode || t.code || '',
    tenantName: t.tenantName || t.name || '',
    contactName: t.contactName || '',
    contactEmail: t.contactEmail || '',
    contactPhone: t.contactPhone || '',
    planType: t.planType || 'standard',
    maxUsers: t.maxUsers || 0,
    maxDatasets: t.maxDatasets || t.maxDatasources || 0,
    maxAiCallsPerDay: t.maxAiCallsPerDay || 0,
    status: t.status ?? 1,
    expireTime: t.expireTime || '',
    createdAt: t.createdAt,
    updatedAt: t.updatedAt,
  }
}

/** Map frontend form fields to backend entity fields */
function mapTenantToBackend(data: TenantForm): Record<string, unknown> {
  return {
    code: data.tenantCode,
    name: data.tenantName,
    contactName: data.contactName,
    contactEmail: data.contactEmail,
    contactPhone: data.contactPhone,
    planType: data.planType,
    maxUsers: data.maxUsers,
    maxDatasets: data.maxDatasets,
    maxAiCallsPerDay: data.maxAiCallsPerDay,
  }
}

export const tenantApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/tenants', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapTenantFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/tenants/${id}`)
    return { ...res, data: mapTenantFromBackend(res.data) }
  },
  create: (data: TenantForm) => post<{ data: any }>('/tenants', mapTenantToBackend(data)),
  update: (id: string, data: TenantForm) => put<{ data: any }>(`/tenants/${id}`, mapTenantToBackend(data)),
  remove: (id: string) => del(`/tenants/${id}`),
}
