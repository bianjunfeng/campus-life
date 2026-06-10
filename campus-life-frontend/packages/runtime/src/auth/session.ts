export interface SessionUser {
  id: string | number
  role: string
}

export interface Session {
  token?: string
  refreshToken?: string
  user?: SessionUser
}

/**
 * Stub session reader. Replaced with shared auth storage in PR-2.
 */
export function getSession(): Session | null {
  return null
}
