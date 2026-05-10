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

/** Transform backend response to frontend format */
function mapWorkflowFromBackend(w: any): Workflow {
  let nodes: WorkflowNode[] = []
  if (w.dagDefinition) {
    try {
      nodes = typeof w.dagDefinition === 'string' ? JSON.parse(w.dagDefinition) : w.dagDefinition
    } catch { nodes = [] }
  }
  return {
    id: w.id,
    name: w.name,
    description: w.description,
    nodes,
    schedule: w.cronExpression || '',
    status: w.status,
    version: w.version,
    createdAt: w.createdAt,
    updatedAt: w.updatedAt,
  }
}

/** Transform frontend form to backend request */
function mapWorkflowToBackend(data: WorkflowForm): Record<string, unknown> {
  return {
    name: data.name,
    description: data.description,
    dagDefinition: JSON.stringify(data.nodes || []),
    triggerType: data.schedule ? 'cron' : 'manual',
    cronExpression: data.schedule || '',
  }
}

function mapRunFromBackend(r: any): WorkflowRun {
  return {
    id: r.id,
    workflowId: r.workflowId,
    workflowName: r.workflowName || `工作流 #${r.workflowId}`,
    status: r.status || r.state || 'pending',
    startedAt: r.startedAt || r.startTime,
    finishedAt: r.finishedAt || r.endTime,
    duration: r.duration || r.durationMs,
    totalTokens: r.totalTokens || 0,
    totalCost: r.totalCost || 0,
    triggerType: r.triggerType || 'manual',
    createdAt: r.createdAt,
  }
}

export const workflowApi = {
  list: async (params: { page: number; pageSize: number; keyword?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/workflows', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapWorkflowFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  detail: async (id: string) => {
    const res = await get<{ data: any }>(`/workflows/${id}`)
    return { ...res, data: mapWorkflowFromBackend(res.data) }
  },
  create: (data: WorkflowForm) => post<{ data: any }>('/workflows', mapWorkflowToBackend(data)),
  update: (id: string, data: WorkflowForm) => put<{ data: any }>(`/workflows/${id}`, mapWorkflowToBackend(data)),
  remove: (id: string) => del(`/workflows/${id}`),
  trigger: (id: string, params?: Record<string, unknown>) =>
    post<{ data: any }>('/workflows/trigger', { workflowId: Number(id), inputParams: params }),
  listRuns: async (params: { page: number; pageSize: number; workflowId?: string; status?: string }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/workflow-runs', params)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapRunFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  getRunDetail: async (runId: string) => {
    const res = await get<{ data: any }>(`/workflow-runs/${runId}`)
    const d = res.data
    return {
      ...res,
      data: {
        ...mapRunFromBackend(d),
        nodeRuns: (d.nodeRuns || []).map((nr: any) => ({
          id: nr.id,
          nodeId: nr.nodeId,
          nodeName: nr.nodeName,
          nodeType: nr.nodeType,
          status: nr.status || nr.state || 'pending',
          startedAt: nr.startedAt || nr.startTime,
          finishedAt: nr.finishedAt || nr.endTime,
          duration: nr.duration || nr.durationMs,
          input: nr.input || nr.inputData,
          output: nr.output || nr.outputData,
          logs: nr.logs || '',
          error: nr.error || nr.errorMessage || '',
          tokensUsed: nr.tokensUsed || 0,
          cost: nr.cost || 0,
        })),
        stateSnapshots: d.stateSnapshots || [],
        error: d.error || d.errorMessage || '',
      },
    }
  },
  resumeRun: (runId: string) => post<{ data: any }>(`/workflow-runs/${runId}/resume`),
}
