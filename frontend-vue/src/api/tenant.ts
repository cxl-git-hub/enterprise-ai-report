import { get, post, put, del } from './request'

export interface Tenant {
  id: string
  name: string
  code: string
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
  create: (data: TenantForm) => post<{ data: Tenant }>('/tenants', data),
  update: (id: string, data: TenantForm) => put<{ data: Tenant }>(`/tenants/${id}`, data),
  remove: (id: string) => del(`/tenants/${id}`),
}
