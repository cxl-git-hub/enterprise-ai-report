import { get } from './request'

export interface AiTrace {
  id: string
  traceId: string
  aiTaskType: string
  modelName: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  cost: number
  latencyMs: number
  status: string
  createdAt: string
}

export interface AiTraceDetail {
  id: string
  traceId: string
  runId: string
  nodeId: string
  aiTaskType: string
  inputPrompt: string
  rawOutput: string
  validatedOutput: string
  validationPassed: boolean
  validationErrors: string[]
  promptTokens: number
  completionTokens: number
  totalTokens: number
  modelName: string
  modelConfig: Record<string, unknown>
  retryCount: number
  latencyMs: number
  cost: number
  status: string
  createdAt: string
}

export const aiTraceApi = {
  list: (params: { page: number; pageSize: number; aiTaskType?: string; status?: string }) =>
    get<{ data: { items: AiTrace[]; total: number } }>('/ai-traces', params),
  detail: (id: string) => get<{ data: AiTraceDetail }>(`/ai-traces/${id}`),
}
