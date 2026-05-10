import { get, post, put, del } from './request'

export interface SchemaColumn {
  name: string
  type: string
  nullable: boolean
  description: string
  example?: string
  businessMeaning?: string
}

export interface Schema {
  id: string
  name: string
  description: string
  datasetId: string
  datasetName: string
  columns: SchemaColumn[]
  version: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface SchemaForm {
  name: string
  description: string
  datasetId: string
  columns: SchemaColumn[]
}

export interface SchemaVersion {
  id: string
  schemaId: string
  version: number
  columns: SchemaColumn[]
  changeNote: string
  createdAt: string
  createdBy: string
}

/** Transform backend response to frontend format */
function mapSchemaFromBackend(s: any): Schema {
  let columns: SchemaColumn[] = []
  if (s.columns) {
    try {
      columns = typeof s.columns === 'string' ? JSON.parse(s.columns) : s.columns
    } catch { columns = [] }
  }
  return {
    id: s.id,
    name: s.name,
    description: s.description,
    datasetId: s.datasetId,
    datasetName: s.datasetName || '',
    columns,
    version: s.version || 1,
    status: s.status || 'draft',
    createdAt: s.createdAt,
    updatedAt: s.updatedAt,
  }
}

/** Transform frontend form to backend request */
function mapSchemaToBackend(data: SchemaForm): Record<string, unknown> {
  return {
    name: data.name,
    description: data.description,
    datasetId: data.datasetId,
    columns: JSON.stringify(data.columns || []),
  }
}

export const schemaApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/schemas', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapSchemaFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/schemas/${id}`)
    return { ...res, data: mapSchemaFromBackend(res.data) }
  },
  create: (data: SchemaForm) => post<{ data: any }>('/schemas', mapSchemaToBackend(data)),
  update: (id: string, data: SchemaForm) => put<{ data: any }>(`/schemas/${id}`, mapSchemaToBackend(data)),
  remove: (id: string) => del(`/schemas/${id}`),
  getVersions: (id: string) => get<{ data: SchemaVersion[] }>(`/schemas/${id}/versions`),
  getVersion: (id: string, version: number) =>
    get<{ data: SchemaVersion }>(`/schemas/${id}/versions/${version}`),
}
