import { get, post, put, del } from './request'

export interface WorkflowNode {
  id: string
  name: string
  type: string
  config: Record<string, unknown>
  dependencies: string[]
}

export interface Workflow {
  id: string
  name: string
  description: string
  nodes: WorkflowNode[]
  schedule: string
  status: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface WorkflowForm {
  name: string
  description: string
  nodes: WorkflowNode[]
  schedule?: string
}

export interface WorkflowRun {
  id: string
  workflowId: string
  workflowName: string
  status: string
  startedAt: string
  finishedAt: string
  duration: number
  totalTokens: number
  totalCost: number
  triggerType: string
  createdAt: string
}

export interface WorkflowNodeRun {
  id: string
  nodeId: string
  nodeName: string
  nodeType: string
  status: string
  startedAt: string
  finishedAt: string
  duration: number
  input: unknown
  output: unknown
  logs: string
  error: string
  tokensUsed: number
  cost: number
}

export interface WorkflowRunDetail {
  id: string
  workflowId: string
  workflowName: string
  status: string
  startedAt: string
  finishedAt: string
  duration: number
  totalTokens: number
  totalCost: number
  triggerType: string
  nodeRuns: WorkflowNodeRun[]
  stateSnapshots: Array<{ state: string; timestamp: string }>
  error: string
}

export const workflowApi = {
  list: (params: { page: number; pageSize: number; keyword?: string }) =>
    get<{ data: { items: Workflow[]; total: number } }>('/workflows', params),
  detail: (id: string) => get<{ data: Workflow }>(`/workflows/${id}`),
  create: (data: WorkflowForm) => post<{ data: Workflow }>('/workflows', data),
  update: (id: string, data: WorkflowForm) => put<{ data: Workflow }>(`/workflows/${id}`, data),
  remove: (id: string) => del(`/workflows/${id}`),
  trigger: (id: string, params?: Record<string, unknown>) =>
    post<{ data: WorkflowRun }>(`/workflows/${id}/trigger`, params),
  listRuns: (params: { page: number; pageSize: number; workflowId?: string; status?: string }) =>
    get<{ data: { items: WorkflowRun[]; total: number } }>('/workflow-runs', params),
  getRunDetail: (runId: string) => get<{ data: WorkflowRunDetail }>(`/workflow-runs/${runId}`),
  resumeRun: (runId: string) => post<{ data: WorkflowRunDetail }>(`/workflow-runs/${runId}/resume`),
}
