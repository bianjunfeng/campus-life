const TOKEN_KEY = 'userToken'
const REFRESH_TOKEN_KEY = 'userRefreshToken'
const USER_INFO_KEY = 'userInfo'
const USER_ROLE_KEY = 'userRole'
const CURRENT_USER_KEY = 'currentUser'
const AUTH_CHANGED_EVENT = 'campus-auth-state-changed'

export function initializeAuthStorage() {
  try {
    const localToken = localStorage.getItem(TOKEN_KEY)
    const localRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    const localUserInfo = localStorage.getItem(USER_INFO_KEY)
    const localUserRole = localStorage.getItem(USER_ROLE_KEY)
    const localCurrentUser = localStorage.getItem(CURRENT_USER_KEY)

    if (!sessionStorage.getItem(TOKEN_KEY) && localToken) {
      sessionStorage.setItem(TOKEN_KEY, localToken)
    }
    if (!sessionStorage.getItem(REFRESH_TOKEN_KEY) && localRefreshToken) {
      sessionStorage.setItem(REFRESH_TOKEN_KEY, localRefreshToken)
    }
    if (!sessionStorage.getItem(USER_INFO_KEY) && localUserInfo) {
      sessionStorage.setItem(USER_INFO_KEY, localUserInfo)
    }
    if (!sessionStorage.getItem(USER_ROLE_KEY) && localUserRole) {
      sessionStorage.setItem(USER_ROLE_KEY, localUserRole)
    }
    if (!sessionStorage.getItem(CURRENT_USER_KEY) && localCurrentUser) {
      sessionStorage.setItem(CURRENT_USER_KEY, localCurrentUser)
    }

    // 清理跨标签页共享的鉴权信息，避免多账号互相覆盖
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
    localStorage.removeItem(USER_ROLE_KEY)
    localStorage.removeItem(CURRENT_USER_KEY)
  } catch (e) {
    console.error('初始化鉴权存储失败:', e)
  }
}

export function getAuthToken(): string {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}

export function setAuthToken(token: string) {
  sessionStorage.setItem(TOKEN_KEY, token)
  emitAuthStateChanged()
}

export function getRefreshToken(): string {
  return sessionStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function setRefreshToken(token: string) {
  sessionStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function clearAuthToken() {
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_KEY)
}

export function clearRefreshToken() {
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function getStoredUserInfoRaw(): string {
  return sessionStorage.getItem(USER_INFO_KEY) || ''
}

export function getStoredUserInfoObject<T = any>(): T | null {
  const raw = getStoredUserInfoRaw()
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export function setStoredUserInfo(user: unknown) {
  sessionStorage.setItem(USER_INFO_KEY, JSON.stringify(user))
}

export function clearStoredUserInfo() {
  sessionStorage.removeItem(USER_INFO_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

export function getStoredUserRole(): string {
  return sessionStorage.getItem(USER_ROLE_KEY) || ''
}

export function setStoredUserRole(role: string) {
  sessionStorage.setItem(USER_ROLE_KEY, role || 'GUEST')
}

export function clearStoredUserRole() {
  sessionStorage.removeItem(USER_ROLE_KEY)
  localStorage.removeItem(USER_ROLE_KEY)
}

export function setCurrentUser(user: unknown) {
  sessionStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user))
}

export function getCurrentUserObject<T = any>(): T | null {
  const raw = sessionStorage.getItem(CURRENT_USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export function clearCurrentUser() {
  sessionStorage.removeItem(CURRENT_USER_KEY)
  localStorage.removeItem(CURRENT_USER_KEY)
}

export function clearAuthState() {
  clearAuthToken()
  clearRefreshToken()
  clearStoredUserInfo()
  clearStoredUserRole()
  clearCurrentUser()
  emitAuthStateChanged()
}

function emitAuthStateChanged() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(AUTH_CHANGED_EVENT))
  }
}
