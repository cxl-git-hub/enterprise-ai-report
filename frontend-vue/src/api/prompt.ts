import { get, post, put, del } from './request'

export interface Prompt {
  id: string
  name: string
  description: string
  templateContent: string
  template: string
  variables: string[] | string
  schemaId: string
  promptType: string
  version: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface PromptForm {
  name: string
  description: string
  templateContent: string
  template?: string
  variables?: string[] | string
  schemaId?: string
  promptType?: string
}

/** Map backend response to frontend format */
function mapPromptFromBackend(p: any): Prompt {
  let variables: string[] = []
  if (p.variables) {
    try {
      variables = typeof p.variables === 'string' ? JSON.parse(p.variables) : p.variables
    } catch { variables = [] }
  }
  return {
    id: p.id,
    name: p.name,
    description: p.description || '',
    templateContent: p.templateContent || '',
    template: p.templateContent || p.template || '',
    variables,
    schemaId: p.schemaId || '',
    promptType: p.promptType || p.config?.promptType || 'general',
    version: p.version || 1,
    status: p.status || 'draft',
    createdAt: p.createdAt,
    updatedAt: p.updatedAt,
  }
}

/** Map frontend form to backend request */
function mapPromptToBackend(data: PromptForm): Record<string, unknown> {
  return {
    name: data.name,
    description: data.description,
    templateContent: data.templateContent || data.template || '',
    variables: JSON.stringify(data.variables || []),
    schemaId: data.schemaId || null,
  }
}

export const promptApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/prompt-templates', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapPromptFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/prompt-templates/${id}`)
    return { ...res, data: mapPromptFromBackend(res.data) }
  },
  create: (data: PromptForm) => post<{ data: any }>('/prompt-templates', mapPromptToBackend(data)),
  update: (id: string, data: PromptForm) => put<{ data: any }>(`/prompt-templates/${id}`, mapPromptToBackend(data)),
  remove: (id: string) => del(`/prompt-templates/${id}`),
}
