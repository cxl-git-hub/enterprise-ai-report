import { get, post, put, del } from './request'

export interface Column {
  name: string
  type: string
  nullable: boolean
  description: string
  isPrimaryKey: boolean
}

export interface Dataset {
  id: string
  name: string
  datasourceId: string
  datasourceName: string
  tableName: string
  description: string
  columns: Column[]
  status: string
  createdAt: string
  updatedAt: string
}

export interface DatasetForm {
  name: string
  datasourceId: string
  tableName: string
  description?: string
}

export const datasetApi = {
  list: (params: { page: number; pageSize: number; keyword?: string }) =>
    get<{ data: { items: Dataset[]; total: number } }>('/datasets', params),
  detail: (id: string) => get<{ data: Dataset }>(`/datasets/${id}`),
  create: (data: DatasetForm) => post<{ data: Dataset }>('/datasets', data),
  update: (id: string, data: DatasetForm) => put<{ data: Dataset }>(`/datasets/${id}`, data),
  remove: (id: string) => del(`/datasets/${id}`),
  getColumns: (id: string) => get<{ data: Column[] }>(`/datasets/${id}/columns`),
  refreshColumns: (id: string) => post<{ data: Column[] }>(`/datasets/${id}/sync-columns`),
  preview: (id: string, limit?: number) =>
    get<{ data: { columns: string[]; rows: Record<string, unknown>[] } }>(`/datasets/${id}/preview`, { limit: limit || 100 }),
}
