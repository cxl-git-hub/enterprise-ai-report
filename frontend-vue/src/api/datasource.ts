import { get, post, put, del } from './request'

export interface DataSource {
  id: string
  name: string
  type: string
  host: string
  port: number
  database: string
  username: string
  status: string
  lastTestAt: string
  lastTestStatus: string
  createdAt: string
  updatedAt: string
}

export interface DataSourceForm {
  name: string
  type: string
  host: string
  port: number
  database: string
  username: string
  password?: string
  extra?: Record<string, unknown>
}

export const datasourceApi = {
  list: (params: { page: number; pageSize: number; keyword?: string }) =>
    get<{ data: { items: DataSource[]; total: number } }>('/datasources', params),
  detail: (id: string) => get<{ data: DataSource }>(`/datasources/${id}`),
  create: (data: DataSourceForm) => post<{ data: DataSource }>('/datasources', data),
  update: (id: string, data: DataSourceForm) => put<{ data: DataSource }>(`/datasources/${id}`, data),
  remove: (id: string) => del(`/datasources/${id}`),
  testConnection: (id: string) => post<{ data: { success: boolean; message: string } }>(`/datasources/${id}/test`),
}
