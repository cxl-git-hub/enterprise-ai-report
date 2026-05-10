import { get, post } from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  email: string
  realName: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tenantId?: string | number
}

export interface UserInfo {
  id: string
  username: string
  email: string
  displayName: string
  realName: string
  tenantId: string
  tenantName: string
  roles: string[]
  permissions: string[]
}

/** Map backend user info to frontend format */
function mapUserFromBackend(u: any): UserInfo {
  return {
    id: u.id,
    username: u.username,
    email: u.email || '',
    displayName: u.displayName || u.realName || u.username || '',
    realName: u.realName || u.displayName || '',
    tenantId: u.tenantId,
    tenantName: u.tenantName || '',
    roles: u.roles || [],
    permissions: u.permissions || [],
  }
}

export const authApi = {
  login: (data: LoginParams) => post<{ data: TokenResponse }>('/auth/login', data),
  register: (data: RegisterParams) => post<{ data: unknown }>('/auth/register', data),
  refreshToken: (data: { refreshToken: string }) => post<{ data: TokenResponse }>('/auth/refresh', data),
  getCurrentUser: async () => {
    const res = await get<{ data: any }>('/auth/me')
    return { ...res, data: mapUserFromBackend(res.data) }
  },
}
