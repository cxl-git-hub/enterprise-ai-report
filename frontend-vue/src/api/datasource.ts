import { get, post, put, del } from './request'

export interface DataSource {
  id: string
  name: string
  type: string
  host: string
  port: number
  database: string
  databaseName: string
  username: string
  status: string | number
  lastTestAt: string
  lastTestStatus: string
  lastTestResult: string
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

/** Map backend entity to frontend format */
function mapDataSourceFromBackend(d: any): DataSource {
  const testResult = d.lastTestResult || ''
  return {
    id: d.id,
    name: d.name,
    type: d.type,
    host: d.host,
    port: d.port,
    database: d.databaseName || d.database || '',
    databaseName: d.databaseName || d.database || '',
    username: d.username || '',
    status: d.status ?? 1,
    lastTestAt: d.lastTestAt || '',
    lastTestStatus: testResult.startsWith('success') ? 'success' : testResult.startsWith('failed') ? 'failed' : '',
    lastTestResult: testResult,
    createdAt: d.createdAt,
    updatedAt: d.updatedAt,
  }
}

export const datasourceApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/datasources', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapDataSourceFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/datasources/${id}`)
    return { ...res, data: mapDataSourceFromBackend(res.data) }
  },
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
    return post<{ data: any }>('/datasources', payload)
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
    return put<{ data: any }>(`/datasources/${id}`, payload)
  },
  remove: (id: string) => del(`/datasources/${id}`),
  testConnection: (id: string) => post<{ data: { success: boolean; message: string; latency: number; version: string } }>(`/datasources/${id}/test`),
}
