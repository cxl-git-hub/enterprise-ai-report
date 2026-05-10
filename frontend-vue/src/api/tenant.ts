import { get, post, put, del } from './request'

export interface Tenant {
  id: string
  name: string           // display name (mapped from tenantName)
  code: string           // mapped from tenantCode
  tenantName: string     // raw backend field
  tenantCode: string     // raw backend field
  description: string
  status: string
  maxUsers: number
  createdAt: string
  updatedAt: string
}

export interface TenantForm {
  name: string
  code: string
  description?: string
  maxUsers?: number
  status?: string
}

export const tenantApi = {
  list: (params: { page: number; pageSize: number; keyword?: string }) =>
    get<{ data: { items: Tenant[]; total: number } }>('/tenants', params),
  detail: (id: string) => get<{ data: Tenant }>(`/tenants/${id}`),
  create: (data: TenantForm) => {
    // Map frontend fields to backend entity fields
    const payload = {
      tenantName: data.name,
      tenantCode: data.code,
      description: data.description,
      maxUsers: data.maxUsers,
      status: data.status === 'active' ? 1 : 0,
    }
    return post<{ data: Tenant }>('/tenants', payload)
  },
  update: (id: string, data: TenantForm) => {
    const payload: Record<string, unknown> = {
      tenantName: data.name,
      description: data.description,
      maxUsers: data.maxUsers,
      status: data.status === 'active' ? 1 : 0,
    }
    return put<{ data: Tenant }>(`/tenants/${id}`, payload)
  },
  remove: (id: string) => del(`/tenants/${id}`),
}
