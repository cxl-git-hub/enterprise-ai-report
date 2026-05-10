import { get, post, put, del } from './request'

export interface ReportTemplate {
  id: string
  name: string
  description: string
  format: string
  templateFile: string
  templateContent: string
  variables: string | unknown[]
  sections: Array<{ name: string; description: string }>
  version: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface ReportTemplateForm {
  name: string
  description: string
  format: string
  templateFile?: string
  templateContent?: string
  variables?: string | unknown[]
  sections?: Array<{ name: string; description: string }>
}

/** Map backend response to frontend format */
function mapTemplateFromBackend(t: any): ReportTemplate {
  let variables: unknown[] = []
  if (t.variables) {
    try {
      variables = typeof t.variables === 'string' ? JSON.parse(t.variables) : t.variables
    } catch { variables = [] }
  }
  return {
    id: t.id,
    name: t.name,
    description: t.description || '',
    format: t.format || 'pdf',
    templateFile: t.templateFile || t.templateContent || '',
    templateContent: t.templateContent || t.templateFile || '',
    variables,
    sections: t.sections || [],
    version: t.version || 1,
    status: t.status || 'draft',
    createdAt: t.createdAt,
    updatedAt: t.updatedAt,
  }
}

/** Map frontend form to backend request */
function mapTemplateToBackend(data: ReportTemplateForm): Record<string, unknown> {
  return {
    name: data.name,
    description: data.description,
    format: data.format,
    templateFile: data.templateFile || data.templateContent || '',
    variables: typeof data.variables === 'string' ? data.variables : JSON.stringify(data.variables || []),
  }
}

export const reportTemplateApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/report-templates', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapTemplateFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/report-templates/${id}`)
    return { ...res, data: mapTemplateFromBackend(res.data) }
  },
  create: (data: ReportTemplateForm) => post<{ data: any }>('/report-templates', mapTemplateToBackend(data)),
  update: (id: string, data: ReportTemplateForm) => put<{ data: any }>(`/report-templates/${id}`, mapTemplateToBackend(data)),
  remove: (id: string) => del(`/report-templates/${id}`),
}
