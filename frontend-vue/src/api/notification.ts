import { get, put, del } from './request'

export interface Notification {
  id: string
  type: string
  title: string
  message: string
  link: string
  isRead: boolean
  sourceType: string
  sourceId: string
  createdAt: string
}

export const notificationApi = {
  list: (params?: { limit?: number; type?: string; read?: boolean }) =>
    get<{ data: Notification[] }>('/notifications', params as Record<string, unknown>),
  unreadCount: () => get<{ data: { count: number } }>('/notifications/unread-count'),
  markRead: (id: string) => put(`/notifications/${id}/read`),
  markAllRead: () => put('/notifications/read-all'),
  clear: () => put('/notifications/clear'),
  remove: (id: string) => del(`/notifications/${id}`),
}
