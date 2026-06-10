import type { PortalId } from '../portal/types.js'

export interface PortalRouterOptions {
  portalId: PortalId
  base?: string
}

/**
 * Stub factory for portal-scoped Vue Router instances.
 * Full implementation arrives in PR-2.
 */
export function createPortalRouter(_options: PortalRouterOptions): unknown {
  throw new Error('createPortalRouter: not implemented')
}
