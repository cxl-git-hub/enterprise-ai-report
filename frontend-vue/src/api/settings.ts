import { get, put } from './request'

export interface AppearanceSettings {
  theme: string
  primaryColor: string
  sidebarPosition: string
  enableAnimation: boolean
  compactMode: boolean
}

export interface AiSettings {
  model: string
  baseUrl: string
  temperature: number
  maxTokens: number
  timeout: number
  dailyLimit: number
}

export interface SecuritySettings {
  sessionTimeout: number
  twoFactor: boolean
}

export interface AdvancedSettings {
  dataRetention: number
  maxConcurrentWorkflows: number
  maxReportStorage: number
  debugMode: boolean
}

export interface SystemSettings {
  appearance: AppearanceSettings
  ai: AiSettings
  notifications: Record<string, unknown>
  security: SecuritySettings
  advanced: AdvancedSettings
}

export const settingsApi = {
  get: () => get<{ data: SystemSettings }>('/settings'),
  saveAppearance: (data: AppearanceSettings) => put('/settings/appearance', data),
  saveAi: (data: AiSettings) => put('/settings/ai', data),
  saveNotifications: (data: Record<string, unknown>) => put('/settings/notifications', data),
  saveSecurity: (data: SecuritySettings) => put('/settings/security', data),
  saveAdvanced: (data: AdvancedSettings) => put('/settings/advanced', data),
  clearCache: () => put('/settings/clear-cache'),
}
