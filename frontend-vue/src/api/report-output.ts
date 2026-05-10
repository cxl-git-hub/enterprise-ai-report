import { get, post } from './request'
import instance from './request'

export interface ReportOutput {
  id: string
  name: string           // mapped from reportName
  reportName: string     // raw backend field
  templateId: string
  templateName: string
  workflowRunId: string
  format: string         // mapped from outputFormat
  outputFormat: string   // raw backend field
  fileSize: number
  status: string
  createdAt: string
  downloadUrl: string
}

export const reportOutputApi = {
  list: (params: { page: number; pageSize: number; keyword?: string; format?: string }) =>
    get<{ data: { items: ReportOutput[]; total: number } }>('/report-outputs', params),
  detail: (id: string) => get<{ data: ReportOutput }>(`/report-outputs/${id}`),
  download: (id: string) => {
    return instance.get(`/report-outputs/${id}/download`, { responseType: 'blob' })
  },
}
