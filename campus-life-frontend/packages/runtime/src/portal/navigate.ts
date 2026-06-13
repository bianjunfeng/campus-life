import { PORTALS } from './registry.js'
import type { PortalId } from './types.js'

function normalizeHost(hostname: string): string {
  return hostname.toLowerCase().replace(/^www\./, '')
}

function hostMatches(configHost: string, hostname: string): boolean {
  const normalized = normalizeHost(hostname)
  const configNormalized = normalizeHost(configHost)
  if (normalized === configNormalized) return true
  const configHostOnly = configNormalized.split(':')[0]
  return normalized === configHostOnly || normalized.startsWith(`${configHostOnly}:`)
}

/**
 * Resolve a portal id from the current or given hostname.
 */
export function resolvePortalByHost(hostname: string): PortalId | null {
  for (const portal of Object.values(PORTALS)) {
    if (hostMatches(portal.devHost, hostname) || hostMatches(portal.prodHost, hostname)) {
      return portal.id
    }
  }
  return null
}

function isDevEnvironment(): boolean {
  if (typeof window !== 'undefined') {
    return window.location.hostname.endsWith('.campus.local')
  }
  return process.env.NODE_ENV !== 'production'
}

function resolvePath(portal: PortalId, path?: string): string {
  const normalized = path ?? PORTALS[portal].defaultRoute
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

/**
 * Build an absolute URL for the given portal and path.
 */
export function portalUrl(portal: PortalId, path?: string, isDev?: boolean): string {
  const config = PORTALS[portal]
  const useDev = isDev ?? isDevEnvironment()
  const host = useDev ? config.devHost : config.prodHost
  const protocol = useDev ? 'http' : 'https'
  return `${protocol}://${host}${resolvePath(portal, path)}`
}

/**
 * Navigate to another portal via full-page redirect.
 */
export function navigatePortal(portal: PortalId, path?: string): void {
  if (typeof window === 'undefined') {
    throw new Error('navigatePortal requires a browser environment')
  }
  window.location.assign(portalUrl(portal, path))
}
