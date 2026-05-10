import { get, post, put, del } from './request'

export interface Prompt {
  id: string
  name: string
  description: string
  template: string
  variables: string[]
  category: string
  version: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface PromptForm {
  name: string
  description: string
  template: string
  variables?: string[]
  category: string
}

export const promptApi = {
  list: (params: { page: number; pageSize: number; keyword?: string; category?: string }) =>
    get<{ data: { items: Prompt[]; total: number } }>('/prompts', params),
  detail: (id: string) => get<{ data: Prompt }>(`/prompts/${id}`),
  create: (data: PromptForm) => post<{ data: Prompt }>('/prompts', data),
  update: (id: string, data: PromptForm) => put<{ data: Prompt }>(`/prompts/${id}`, data),
  remove: (id: string) => del(`/prompts/${id}`),
}
