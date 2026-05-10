import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

interface User {
  id: string
  username: string
  email: string
  realName: string
  displayName?: string
  tenantId: string
  tenantName?: string
  createdAt?: string
  roles: string[]
  permissions: string[]
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshTokenValue = ref<string>(localStorage.getItem('refreshToken') || '')
  const user = ref<User | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const permissions = computed(() => user.value?.permissions || [])
  const roles = computed(() => user.value?.roles || [])
  const displayName = computed(() => user.value?.realName || user.value?.username || '')

  function setToken(accessToken: string, refresh: string) {
    token.value = accessToken
    refreshTokenValue.value = refresh
    localStorage.setItem('token', accessToken)
    localStorage.setItem('refreshToken', refresh)
  }

  function clearAuth() {
    token.value = ''
    refreshTokenValue.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('tenantId')
    localStorage.removeItem('permissions')
    localStorage.removeItem('roles')
  }

  async function login(username: string, password: string) {
    loading.value = true
    try {
      const res = await authApi.login({ username, password })
      setToken(res.data.accessToken, res.data.refreshToken)
      localStorage.setItem('tenantId', String(res.data.tenantId || ''))
      await fetchCurrentUser()
    } finally {
      loading.value = false
    }
  }

  async function fetchCurrentUser() {
    try {
      const res = await authApi.getCurrentUser()
      // Map realName to displayName for frontend compatibility
      const userData = res.data
      if (userData.realName && !userData.displayName) {
        userData.displayName = userData.realName
      }
      user.value = userData
      // Persist permissions and roles for route guards
      localStorage.setItem('permissions', JSON.stringify(userData.permissions || []))
      localStorage.setItem('roles', JSON.stringify(userData.roles || []))
    } catch {
      clearAuth()
    }
  }

  async function refreshToken() {
    try {
      const res = await authApi.refreshToken({ refreshToken: refreshTokenValue.value })
      setToken(res.data.accessToken, res.data.refreshToken)
      return res.data.accessToken
    } catch {
      clearAuth()
      throw new Error('Token refresh failed')
    }
  }

  function logout() {
    clearAuth()
  }

  function hasPermission(perm: string): boolean {
    if (roles.value.includes('SUPER_ADMIN')) return true
    if (permissions.value.includes('*')) return true
    return permissions.value.includes(perm)
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  return {
    token,
    user,
    loading,
    isLoggedIn,
    permissions,
    roles,
    displayName,
    login,
    logout,
    fetchCurrentUser,
    refreshToken,
    hasPermission,
    hasRole,
    setToken,
    clearAuth,
  }
})
