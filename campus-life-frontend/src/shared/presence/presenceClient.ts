import { heartbeatPresence, offlinePresence } from '../api/presence'
import { getAuthToken } from '../utils/authStorage'

const HEARTBEAT_INTERVAL_MS = 30_000
const PORTAL_KEY = 'campusPortal'
const AUTH_CHANGED_EVENT = 'campus-auth-state-changed'

let heartbeatTimer: number | null = null
let started = false
let portalName = 'consumer'

export function startPresenceClient(portal: string) {
  if (typeof window === 'undefined') {
    return
  }
  portalName = portal || portalName
  sessionStorage.setItem(PORTAL_KEY, portalName)

  if (!started) {
    started = true
    window.addEventListener('focus', heartbeatNow)
    window.addEventListener(AUTH_CHANGED_EVENT, heartbeatNow)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('pagehide', sendOfflineKeepalive)
    window.addEventListener('beforeunload', sendOfflineKeepalive)
  }

  if (heartbeatTimer == null) {
    heartbeatTimer = window.setInterval(heartbeatNow, HEARTBEAT_INTERVAL_MS)
  }
  void heartbeatNow()
}

export function stopPresenceClient() {
  if (heartbeatTimer != null) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

export async function heartbeatNow() {
  if (!getAuthToken()) {
    return
  }
  try {
    await heartbeatPresence(portalName)
  } catch {
    // Presence is best-effort and must not interrupt product flows.
  }
}

export async function markPresenceOffline() {
  if (!getAuthToken()) {
    return
  }
  try {
    await offlinePresence(portalName)
  } catch {
    // Logout/token revocation paths also clear server-side presence.
  }
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    void heartbeatNow()
  }
}

function sendOfflineKeepalive() {
  const token = getAuthToken()
  if (!token) {
    return
  }
  try {
    void fetch('/api/presence/offline', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ portal: portalName }),
      keepalive: true
    })
  } catch {
    // Browsers may drop unload requests; Redis TTL remains the source of truth.
  }
}

export function notifyAuthStateChanged() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(AUTH_CHANGED_EVENT))
  }
}

export function getPresencePortal() {
  return sessionStorage.getItem(PORTAL_KEY) || portalName
}
