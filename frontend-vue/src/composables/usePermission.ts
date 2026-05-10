import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function usePermission() {
  const authStore = useAuthStore()

  const hasPermission = (permission: string): boolean => {
    return authStore.hasPermission(permission)
  }

  const hasAnyPermission = (permissions: string[]): boolean => {
    return permissions.some((p) => authStore.hasPermission(p))
  }

  const hasAllPermissions = (permissions: string[]): boolean => {
    return permissions.every((p) => authStore.hasPermission(p))
  }

  const hasRole = (role: string): boolean => {
    return authStore.hasRole(role)
  }

  const isAdmin = computed(() => authStore.hasRole('admin'))

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    isAdmin,
  }
}
