import { get, post, put, del } from './request'

export interface Kpi {
  id: string
  name: string
  description: string
  expression: string
  unit: string
  aggregationType: string
  schemaId: string
  schemaName: string
  version: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface KpiForm {
  name: string
  description: string
  expression: string
  unit?: string
  aggregationType: string
  schemaId: string
}

export interface KpiVersion {
  id: string
  kpiId: string
  version: number
  expression: string
  changeNote: string
  createdAt: string
  createdBy: string
}

export interface KpiExecuteResult {
  value: number
  unit: string
  executedAt: string
  duration: number
}

export const kpiApi = {
  list: (params: { page: number; pageSize: number; keyword?: string }) =>
    get<{ data: { items: Kpi[]; total: number } }>('/kpis', params),
  detail: (id: string) => get<{ data: Kpi }>(`/kpis/${id}`),
  create: (data: KpiForm) => post<{ data: Kpi }>('/kpis', data),
  update: (id: string, data: KpiForm) => put<{ data: Kpi }>(`/kpis/${id}`, data),
  remove: (id: string) => del(`/kpis/${id}`),
  execute: (id: string, params?: Record<string, unknown>) =>
    post<{ data: KpiExecuteResult }>(`/kpis/${id}/execute`, params),
  getVersions: (id: string) => get<{ data: KpiVersion[] }>(`/kpis/${id}/versions`),
  getVersion: (id: string, version: number) =>
    get<{ data: KpiVersion }>(`/kpis/${id}/versions/${version}`),
}
