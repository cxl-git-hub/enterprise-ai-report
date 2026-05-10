import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import NProgress from 'nprogress'

const whiteList = ['/login']

export async function authGuard(
  to: RouteLocationNormalized,
  _from: RouteLocationNormalized,
  next: NavigationGuardNext
) {
  NProgress.start()
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth === false) {
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  if (!token) {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
    return
  }

  // Check permission
  const requiredPermission = to.meta.permission as string | undefined
  if (requiredPermission) {
    try {
      const permissionsStr = localStorage.getItem('permissions') || '[]'
      const permissions: string[] = JSON.parse(permissionsStr)
      const rolesStr = localStorage.getItem('roles') || '[]'
      const roles: string[] = JSON.parse(rolesStr)
      if (!roles.includes('admin') && !permissions.includes(requiredPermission)) {
        next('/dashboard')
        return
      }
    } catch {
      // If permissions not available yet, allow access
    }
  }

  next()
}

export function afterEach(_to: RouteLocationNormalized, _from: RouteLocationNormalized) {
  NProgress.done()
}
