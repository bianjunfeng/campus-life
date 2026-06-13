export type {
  MountPortalOptions,
  PortalConfig,
  PortalContext,
  PortalId,
  PortalRole,
  PortalRoutes
} from './portal/types.js'

export { PORTALS } from './portal/registry.js'
export { navigatePortal, portalUrl, resolvePortalByHost } from './portal/navigate.js'
export { portalContextKey, providePortal, usePortal } from './portal/usePortal.js'

export { createPortalRouter } from './router/createPortalRouter.js'
export type { PortalRouterOptions } from './router/createPortalRouter.js'
export { portalGuard } from './router/guards.js'
export type { PortalGuardContext } from './router/guards.js'

export { getSession } from './auth/session.js'
export type { Session, SessionUser } from './auth/session.js'

export { createHttpClient } from './http/client.js'
export type { HttpClient, HttpClientOptions } from './http/client.js'

export { mountPortal } from './createCampusApp.js'
