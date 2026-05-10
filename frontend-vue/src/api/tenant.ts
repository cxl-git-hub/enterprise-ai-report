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

export const tenantApi = {
  list: (params: { page: number; pageSize: number; keyword?: string }) =>
    get<{ data: { items: Tenant[]; total: number } }>('/tenants', params),
  detail: (id: string) => get<{ data: Tenant }>(`/tenants/${id}`),
  create: (data: TenantForm) => post<{ data: Tenant }>('/tenants', data),
  update: (id: string, data: TenantForm) => put<{ data: Tenant }>(`/tenants/${id}`, data),
  remove: (id: string) => del(`/tenants/${id}`),
}
