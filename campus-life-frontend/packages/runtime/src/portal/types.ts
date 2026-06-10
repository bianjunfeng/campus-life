export type PortalId = 'STUDENT' | 'MERCHANT' | 'ADMIN'

export type PortalRole = 'STUDENT' | 'MERCHANT' | 'ADMIN'

/** Canonical route paths for a portal (Scheme C, no legacy prefix). */
export interface PortalRoutes {
  agent: string
  account: string
}

export interface PortalConfig {
  id: PortalId
  requiredRole: PortalRole
  devHost: string
  prodHost: string
  defaultRoute: string
  routes: PortalRoutes
  /** Optional gate keys evaluated before route access (e.g. merchantVerified). */
  gates?: string[]
}

/** Injected portal context available to portal-scoped components. */
export interface PortalContext {
  portalId: PortalId
  config: PortalConfig
}

/** Options passed to {@link mountPortal} when bootstrapping a portal app. */
export interface MountPortalOptions {
  portalId: PortalId
  rootSelector?: string
}
