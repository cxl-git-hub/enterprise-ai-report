import { get, post } from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  email: string
  displayName: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface UserInfo {
  id: string
  username: string
  email: string
  displayName: string  // mapped from realName
  realName: string
  tenantId: string
  tenantName: string
  roles: string[]
  permissions: string[]
}

export const authApi = {
  login: (data: LoginParams) => post<{ data: TokenResponse }>('/auth/login', data),
  register: (data: RegisterParams) => post<{ data: unknown }>('/auth/register', data),
  refreshToken: (data: { refreshToken: string }) => post<{ data: TokenResponse }>('/auth/refresh', data),
  getCurrentUser: () => get<{ data: UserInfo }>('/auth/me'),
}
