import type { NavigationGuard } from 'vue-router'
import type { PortalId } from '../portal/types.js'

export interface PortalGuardContext {
  portalId: PortalId
}

/**
 * Stub portal guard matching the future Scheme C design.
 * Composes auth, role, and gate checks before route activation.
 */
export function portalGuard(_context: PortalGuardContext): NavigationGuard {
  return (_to, _from, next) => {
    next()
  }
}
