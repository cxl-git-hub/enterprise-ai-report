import { get, post } from './request'
import instance from './request'

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

/** Map backend snapshot to frontend format */
function mapSnapshotFromBackend(s: any): Snapshot {
  return {
    id: s.id,
    name: s.snapshotName || s.name || '',
    description: s.description || '',
    configHash: s.snapshotVersion || s.configHash || '',
    itemCount: s.itemCount || 0,
    createdAt: s.createdAt,
    createdBy: s.createdBy || '',
  }
}

export const configApi = {
  validate: () => post<{ data: ValidationResult }>('/config/validate'),
  getDependencyGraph: () => get<{ data: DependencyGraph }>('/config/dependency-graph'),
  createSnapshot: (data: { name: string; description?: string }) =>
    post<{ data: any }>('/config/snapshots', data),
  listSnapshots: async (params?: { page: number; pageSize: number }) => {
    const res = await get<{ data: { items: any[]; total: number } }>('/config/snapshots', params as Record<string, unknown>)
    return {
      ...res,
      data: {
        items: (res.data?.items || []).map(mapSnapshotFromBackend),
        total: res.data?.total || 0,
      },
    }
  },
  restoreSnapshot: (id: string) => post<{ data: unknown }>(`/config/snapshots/${id}/restore`),
  getSnapshotDetail: async (id: string) => {
    const res = await get<{ data: any }>(`/config/snapshots/${id}`)
    return { ...res, data: mapSnapshotFromBackend(res.data) }
  },
  diffSnapshots: (id1: string, id2: string) =>
    get<{ data: { changes: Array<{ type: string; path: string; old: unknown; new: unknown }> } }>(
      '/config/snapshots/diff',
      { id1, id2 }
    ),
  exportConfig: () => instance.get('/config/export', { responseType: 'blob' }),
  importConfig: (file: File, merge?: boolean) => {
    const formData = new FormData()
    formData.append('file', file)
    if (merge !== undefined) formData.append('merge', String(merge))
    return instance.post('/config/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
