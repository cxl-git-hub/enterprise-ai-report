import { get } from './request'
import instance from './request'

export interface ReportOutput {
  id: string
  name: string
  reportName: string
  templateId: string
  templateName: string
  workflowRunId: string
  format: string
  outputFormat: string
  fileSize: number
  status: string
  createdAt: string
  downloadUrl: string
}

/** Map backend entity to frontend interface */
function mapReportFromBackend(r: any): ReportOutput {
  return {
    id: r.id,
    name: r.name || r.reportName || '',
    reportName: r.reportName || r.name || '',
    templateId: r.reportTemplateId || r.templateId || '',
    templateName: r.templateName || '',
    workflowRunId: r.workflowRunId || '',
    format: (r.format || r.outputFormat || '').toString().toLowerCase(),
    outputFormat: r.outputFormat || r.format || '',
    fileSize: r.fileSize || 0,
    status: r.status != null ? String(r.status) : '',
    createdAt: r.createdAt,
    downloadUrl: r.downloadUrl || '',
  }
}

export const reportOutputApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string; format?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/report-outputs', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapReportFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/report-outputs/${id}`)
    return { ...res, data: mapReportFromBackend(res.data) }
  },
  download: (id: string) => {
    return instance.get(`/report-outputs/${id}/download`, { responseType: 'blob' })
  },
}
