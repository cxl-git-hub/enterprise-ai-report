import { get, post } from './request'

export interface ValidationResult {
  valid: boolean
  errors: Array<{ type: string; message: string; refId: string; refName: string }>
  warnings: Array<{ type: string; message: string; refId: string; refName: string }>
}

export interface Snapshot {
  id: string
  name: string
  description: string
  configHash: string
  itemCount: number
  createdAt: string
  createdBy: string
}

export interface DependencyNode {
  id: string
  name: string
  type: string
  dependencies: string[]
}

export interface DependencyGraph {
  nodes: DependencyNode[]
  edges: Array<{ from: string; to: string; type: string }>
}

export const configApi = {
  validate: () => post<{ data: ValidationResult }>('/config/validate'),
  getDependencyGraph: () => get<{ data: DependencyGraph }>('/config/dependency-graph'),
  createSnapshot: (data: { name: string; description?: string }) =>
    post<{ data: Snapshot }>('/config/snapshots', data),
  listSnapshots: (params?: { page: number; pageSize: number }) =>
    get<{ data: { items: Snapshot[]; total: number } }>('/config/snapshots', params),
  restoreSnapshot: (id: string) => post<{ data: unknown }>(`/config/snapshots/${id}/restore`),
  getSnapshotDetail: (id: string) => get<{ data: Snapshot }>(`/config/snapshots/${id}`),
  diffSnapshots: (id1: string, id2: string) =>
    get<{ data: { changes: Array<{ type: string; path: string; old: unknown; new: unknown }> } }>(
      '/config/snapshots/diff',
      { id1, id2 }
    ),
}
