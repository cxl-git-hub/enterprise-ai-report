import { get, post, put, del } from './request'

export interface Column {
  columnName: string
  columnType: string
  isNullable: number
  description: string
  isPrimaryKey: number
  displayName: string
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

/** Map backend response to frontend format */
function mapDatasetFromBackend(d: any): Dataset {
  return {
    id: d.id,
    name: d.name,
    datasourceId: d.dataSourceId || d.datasourceId || '',
    datasourceName: d.datasourceName || d.dataSourceName || '',
    tableName: d.tableName || '',
    description: d.description || '',
    columns: d.columns || [],
    status: d.status != null ? String(d.status) : 'draft',
    createdAt: d.createdAt,
    updatedAt: d.updatedAt,
  }
}

export const datasetApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/datasets', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapDatasetFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/datasets/${id}`)
    return { ...res, data: mapDatasetFromBackend(res.data) }
  },
  create: (data: DatasetForm) => post<{ data: any }>('/datasets', {
    name: data.name,
    dataSourceId: data.datasourceId,
    tableName: data.tableName,
    description: data.description,
  }),
  update: (id: string, data: DatasetForm) => put<{ data: any }>(`/datasets/${id}`, {
    name: data.name,
    dataSourceId: data.datasourceId,
    tableName: data.tableName,
    description: data.description,
  }),
  remove: (id: string) => del(`/datasets/${id}`),
  getColumns: (id: string) => get<{ data: Column[] }>(`/datasets/${id}/columns`),
  refreshColumns: (id: string) => post<{ data: Column[] }>(`/datasets/${id}/sync-columns`),
  preview: (id: string, limit?: number) =>
    get<{ data: { columns: string[]; rows: Record<string, unknown>[] } }>(`/datasets/${id}/preview`, { limit: limit || 100 }),
}
