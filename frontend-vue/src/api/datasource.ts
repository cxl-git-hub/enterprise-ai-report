import { get, post, put, del } from './request'

export interface DataSource {
  id: string
  name: string
  type: string
  host: string
  port: number
  database: string      // mapped from databaseName
  databaseName: string  // raw backend field
  username: string
  status: string | number
  lastTestAt: string
  lastTestStatus: string // mapped from lastTestResult
  lastTestResult: string // raw backend field
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
  create: (data: DataSourceForm) => {
    const payload = {
      name: data.name,
      type: data.type,
      host: data.host,
      port: data.port,
      databaseName: data.database,
      username: data.username,
      encryptedPassword: data.password,
    }
    return post<{ data: DataSource }>('/datasources', payload)
  },
  update: (id: string, data: DataSourceForm) => {
    const payload: Record<string, unknown> = {
      name: data.name,
      type: data.type,
      host: data.host,
      port: data.port,
      databaseName: data.database,
      username: data.username,
    }
    if (data.password) {
      payload.encryptedPassword = data.password
    }
    return put<{ data: DataSource }>(`/datasources/${id}`, payload)
  },
  remove: (id: string) => del(`/datasources/${id}`),
  testConnection: (id: string) => post<{ data: { success: boolean; message: string } }>(`/datasources/${id}/test`),
}
