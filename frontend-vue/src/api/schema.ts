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

export const schemaApi = {
  list: (params: { page: number; pageSize: number; keyword?: string }) =>
    get<{ data: { items: Schema[]; total: number } }>('/schemas', params),
  detail: (id: string) => get<{ data: Schema }>(`/schemas/${id}`),
  create: (data: SchemaForm) => post<{ data: Schema }>('/schemas', data),
  update: (id: string, data: SchemaForm) => put<{ data: Schema }>(`/schemas/${id}`, data),
  remove: (id: string) => del(`/schemas/${id}`),
  getVersions: (id: string) => get<{ data: SchemaVersion[] }>(`/schemas/${id}/versions`),
  getVersion: (id: string, version: number) =>
    get<{ data: SchemaVersion }>(`/schemas/${id}/versions/${version}`),
}
