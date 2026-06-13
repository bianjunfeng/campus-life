import type { PortalConfig, PortalId } from './types.js'

export const PORTALS: Record<PortalId, PortalConfig> = {
  STUDENT: {
    id: 'STUDENT',
    requiredRole: 'STUDENT',
    devHost: 'life.campus.local:5173',
    prodHost: 'life.example.com',
    defaultRoute: '/home',
    routes: {
      agent: '/agent',
      account: '/me'
    }
  },
  MERCHANT: {
    id: 'MERCHANT',
    requiredRole: 'MERCHANT',
    devHost: 'merchant.campus.local:5175',
    prodHost: 'merchant.example.com',
    defaultRoute: '/home',
    routes: {
      agent: '/ai',
      account: '/profile/info'
    },
    gates: ['merchantVerified']
  },
  ADMIN: {
    id: 'ADMIN',
    requiredRole: 'ADMIN',
    devHost: 'admin.campus.local:5174',
    prodHost: 'admin.example.com',
    defaultRoute: '/dashboard',
    routes: {
      agent: '/ai/agents',
      account: '/profile/info'
    }
  }
}
