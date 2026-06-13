import type { PortalContext } from './types.js'

export const portalContextKey = Symbol('portalContext')

/** Stub: provide portal context to descendant components. */
export function providePortal(_context: PortalContext): void {
  // Wired in PR-2 when runtime mounts Vue apps.
}

/** Stub: read portal context from an ancestor provider. */
export function usePortal(): PortalContext {
  throw new Error('usePortal: portal context not provided')
}
