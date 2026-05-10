import { get, post, put, del } from './request'

export interface User {
  id: string
  username: string
  email: string
  realName: string
  phone: string
  tenantId: string
  status: number
  roles: string[]
  createdAt: string
  updatedAt: string
}

export interface UserForm {
  username: string
  email: string
  realName: string
  phone?: string
  password?: string
  status?: number
  roleIds?: string[]
}

export const userApi = {
  list: (params: { page: number; pageSize: number; keyword?: string; tenantId?: string }) =>
    get<{ data: { items: User[]; total: number } }>('/users', params),
  detail: (id: string) => get<{ data: User }>(`/users/${id}`),
  create: (data: UserForm) => post<{ data: User }>('/users', data),
  update: (id: string, data: UserForm) => put<{ data: User }>(`/users/${id}`, data),
  remove: (id: string) => del(`/users/${id}`),
  assignRoles: (id: string, roleIds: string[]) => post(`/users/${id}/roles`, { roleIds }),
}
