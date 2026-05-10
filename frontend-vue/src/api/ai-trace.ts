import { get } from './request'

export interface AiTrace {
  id: string
  traceType: string
  modelName: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  cost: number
  duration: number
  status: string
  createdAt: string
}

export interface AiTraceDetail {
  id: string
  traceType: string
  modelName: string
  fullPrompt: string
  rawOutput: string
  validatedOutput: string
  validationErrors: string[]
  promptTokens: number
  completionTokens: number
  totalTokens: number
  cost: number
  duration: number
  status: string
  retries: Array<{ attempt: number; output: string; error: string; tokens: number; timestamp: string }>
  metadata: Record<string, unknown>
  createdAt: string
}

export const aiTraceApi = {
  list: (params: { page: number; pageSize: number; traceType?: string; status?: string }) =>
    get<{ data: { items: AiTrace[]; total: number } }>('/ai/traces', params),
  detail: (id: string) => get<{ data: AiTraceDetail }>(`/ai/traces/${id}`),
}
