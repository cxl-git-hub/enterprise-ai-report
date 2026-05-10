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
  unit?: string
  executedAt?: string
  duration?: number
  kpiCode?: string
  dimensions?: Record<string, unknown[]>
  calculatedAt?: string
}

export interface KpiTrendPoint {
  date: string
  value: number
  formattedValue?: string
  executedAt?: string
}

/** Map backend KPI to frontend format */
function mapKpiFromBackend(k: any): Kpi {
  return {
    id: k.id,
    name: k.name,
    description: k.description || '',
    expression: k.expression || '',
    unit: k.unit || '',
    aggregationType: k.aggregationType || '',
    schemaId: k.schemaId || '',
    schemaName: k.schemaName || '',
    version: k.version || 1,
    status: k.status || 'draft',
    createdAt: k.createdAt,
    updatedAt: k.updatedAt,
  }
}

export const kpiApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/kpis', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapKpiFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/kpis/${id}`)
    return { ...res, data: mapKpiFromBackend(res.data) }
  },
  create: (data: KpiForm) => post<{ data: any }>('/kpis', {
    name: data.name,
    description: data.description,
    expression: data.expression,
    unit: data.unit,
    aggregationType: data.aggregationType,
    schemaId: data.schemaId,
    datasetId: null,
  }),
  update: (id: string, data: KpiForm) => put<{ data: any }>(`/kpis/${id}`, {
    name: data.name,
    description: data.description,
    expression: data.expression,
    unit: data.unit,
    aggregationType: data.aggregationType,
    schemaId: data.schemaId,
  }),
  remove: (id: string) => del(`/kpis/${id}`),
  execute: (id: string, params?: Record<string, unknown>) =>
    post<{ data: KpiExecuteResult }>(`/kpis/${id}/execute`, params || {}),
  getVersions: (id: string) => get<{ data: KpiVersion[] }>(`/kpis/${id}/versions`),
  getVersion: (id: string, version: number) =>
    get<{ data: KpiVersion }>(`/kpis/${id}/versions/${version}`),
  getTrend: (id: string, params?: { startDate?: string; endDate?: string; limit?: number }) =>
    get<{ data: KpiTrendPoint[] }>(`/kpis/${id}/trend`, params as Record<string, unknown>),
}
