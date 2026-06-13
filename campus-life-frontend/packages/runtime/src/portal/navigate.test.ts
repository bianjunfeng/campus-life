import { describe, expect, it } from 'vitest'
import { portalUrl, resolvePortalByHost } from './navigate.js'

describe('resolvePortalByHost', () => {
  it('resolves dev hosts with port', () => {
    expect(resolvePortalByHost('life.campus.local:5173')).toBe('STUDENT')
    expect(resolvePortalByHost('merchant.campus.local:5175')).toBe('MERCHANT')
    expect(resolvePortalByHost('admin.campus.local:5174')).toBe('ADMIN')
  })

  it('resolves dev hosts without port', () => {
    expect(resolvePortalByHost('life.campus.local')).toBe('STUDENT')
    expect(resolvePortalByHost('merchant.campus.local')).toBe('MERCHANT')
  })

  it('resolves production hosts', () => {
    expect(resolvePortalByHost('life.example.com')).toBe('STUDENT')
    expect(resolvePortalByHost('merchant.example.com')).toBe('MERCHANT')
    expect(resolvePortalByHost('admin.example.com')).toBe('ADMIN')
  })

  it('returns null for unknown hosts', () => {
    expect(resolvePortalByHost('unknown.example.com')).toBeNull()
    expect(resolvePortalByHost('localhost')).toBeNull()
  })
})

describe('portalUrl', () => {
  it('builds dev URLs with default route', () => {
    expect(portalUrl('STUDENT', undefined, true)).toBe('http://life.campus.local:5173/home')
    expect(portalUrl('MERCHANT', undefined, true)).toBe('http://merchant.campus.local:5175/home')
  })

  it('builds prod URLs with explicit path', () => {
    expect(portalUrl('ADMIN', '/ai/agents', false)).toBe('https://admin.example.com/ai/agents')
    expect(portalUrl('STUDENT', '/me', false)).toBe('https://life.example.com/me')
  })

  it('normalizes paths without leading slash', () => {
    expect(portalUrl('MERCHANT', 'profile/info', true)).toBe('http://merchant.campus.local:5175/profile/info')
  })
})
