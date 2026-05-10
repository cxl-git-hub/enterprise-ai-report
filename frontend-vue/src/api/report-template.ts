import { get, post, put, del } from './request'

export interface ReportTemplate {
  id: string
  name: string
  description: string
  format: string
  templateFile: string
  variables: string
  version: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface ReportTemplateForm {
  name: string
  description: string
  format: string
  templateFile: string
  variables?: string
}

export const reportTemplateApi = {
  list: (params: { page: number; pageSize: number; keyword?: string }) =>
    get<{ data: { items: ReportTemplate[]; total: number } }>('/report-templates', params),
  detail: (id: string) => get<{ data: ReportTemplate }>(`/report-templates/${id}`),
  create: (data: ReportTemplateForm) => post<{ data: ReportTemplate }>('/report-templates', data),
  update: (id: string, data: ReportTemplateForm) => put<{ data: ReportTemplate }>(`/report-templates/${id}`, data),
  remove: (id: string) => del(`/report-templates/${id}`),
}
