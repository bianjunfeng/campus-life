import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import http from '../api/http'
import {
  clearAuthState,
  getCurrentUserObject,
  getAuthToken,
  getRefreshToken,
  getStoredUserInfoObject,
  getStoredUserRole,
  setAuthToken,
  setCurrentUser,
  setRefreshToken,
  setStoredUserInfo,
  setStoredUserRole
} from '../utils/authStorage'

type PortalRole = 'STUDENT' | 'MERCHANT' | 'ADMIN'

const roleRequirementReason: Record<PortalRole, string> = {
  STUDENT: 'student-required',
  MERCHANT: 'merchant-required',
  ADMIN: 'admin-required'
}

function normalizeRole(role: unknown) {
  const normalizedRole = String(role ?? '').toUpperCase()
  return normalizedRole.startsWith('ROLE_') ? normalizedRole.slice(5) : normalizedRole
}

function parseTokenPayload(rawToken?: string) {
  if (!rawToken) return null
  try {
    const payload = rawToken.split('.')[1]
    if (!payload) return null
    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/')
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '=')
    return JSON.parse(atob(paddedPayload))
  } catch {
    return null
  }
}

function isTokenValid(rawToken?: string) {
  const tokenPayload = parseTokenPayload(rawToken)
  return !!tokenPayload?.exp && Date.now() < tokenPayload.exp * 1000
}

export function checkLoginStatus() {
  return isTokenValid(getAuthToken()) || isTokenValid(getRefreshToken())
}

export function getUserRoles() {
  let isStudent = false
  let isAdmin = false
  let isMerchant = false

  const applyRole = (role: unknown) => {
    const normalizedRole = normalizeRole(role)
    isStudent = isStudent || normalizedRole === 'STUDENT' || normalizedRole === '0'
    isAdmin = isAdmin || normalizedRole === 'ADMIN' || normalizedRole === '2'
    isMerchant = isMerchant || normalizedRole === 'MERCHANT' || normalizedRole === '1'
  }

  for (const role of [
    getStoredUserInfoObject<any>()?.role,
    getCurrentUserObject<any>()?.role,
    getStoredUserRole(),
    parseTokenPayload(getAuthToken())?.role
  ]) {
    if (role !== undefined && role !== null) {
      applyRole(role)
    }
  }

  return { isStudent, isAdmin, isMerchant }
}

export function saveLoginState(token: string, user: any, refreshToken?: string) {
  setAuthToken(token)
  if (refreshToken) {
    setRefreshToken(refreshToken)
  }
  setStoredUserRole(user.role || 'GUEST')
  setStoredUserInfo(user)
  setCurrentUser(user)
}

export function clearLoginState() {
  clearAuthState()
  localStorage.removeItem('redirectAfterLogin')
}

export function createAuthGuard(fallbackRouteName: string, portalRole?: PortalRole) {
  return async (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)
    const isLoggedIn = checkLoginStatus()
    const requiresStudent = to.matched.some(record => record.meta.requiresStudent)
    const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin)
    const requiresMerchant = to.matched.some(record => record.meta.requiresMerchant)
    const isAuthEntry = to.name === 'StudentAuth' || to.name === 'MerchantAuth'
    const isPublicAuthRoute = to.name === 'Login'
      || to.name === 'Register'
      || to.name === 'WechatCallback'
      || to.name === 'QQCallback'
    const { isStudent, isAdmin, isMerchant } = getUserRoles()
    const portalRoleMatched = !portalRole
      || (portalRole === 'STUDENT' && isStudent)
      || (portalRole === 'MERCHANT' && isMerchant)
      || (portalRole === 'ADMIN' && isAdmin)

    const rejectCurrentPortal = (reason: string) => {
      clearAuthState()
      localStorage.setItem('redirectAfterLogin', to.fullPath)
      localStorage.setItem('requireLogin', 'true')
      return next({ name: 'Login', query: { reason } })
    }

    if (!isLoggedIn && requiresAuth) {
      localStorage.setItem('redirectAfterLogin', to.fullPath)
      localStorage.setItem('requireLogin', 'true')
      return next({ name: 'Login' })
    }

    if (isLoggedIn) {
      if (portalRole && !isPublicAuthRoute && !portalRoleMatched) {
        return rejectCurrentPortal(roleRequirementReason[portalRole])
      }

      if (isAuthEntry) {
        if (isAdmin) {
          return next({ name: fallbackRouteName })
        }

        try {
          const { data } = await http.get('/users/me/auth-status/any')
          if (data?.code === 200 && data?.data?.hasAnyAuth === true) {
            return next({ name: fallbackRouteName })
          }
        } catch {
          // 认证状态接口失败时保持现有登录态放行
        }
      }

      if (requiresStudent && !isStudent) {
        return rejectCurrentPortal('student-required')
      }

      if (requiresAdmin && !isAdmin) {
        return rejectCurrentPortal('admin-required')
      }

      if (requiresMerchant && !isMerchant) {
        return rejectCurrentPortal('merchant-required')
      }

      if (requiresMerchant) {
        try {
          const { data } = await http.get('/users/me/auth-status/any')
          const verified = data?.code === 200 && data?.data?.merchantVerified === true
          if (!verified) {
            return rejectCurrentPortal('merchant-unverified')
          }
        } catch {
          return rejectCurrentPortal('merchant-unverified')
        }
      }
    }

    localStorage.removeItem('requireLogin')
    next()
  }
}
