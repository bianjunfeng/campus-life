import { describe, expect, it } from 'vitest'
import { PORTALS } from './registry.js'
import type { PortalId } from './types.js'

describe('PORTALS registry', () => {
  it('defines all three portal ids', () => {
    const ids = Object.keys(PORTALS) as PortalId[]
    expect(ids).toEqual(expect.arrayContaining(['STUDENT', 'MERCHANT', 'ADMIN']))
    expect(ids).toHaveLength(3)
  })

  it('configures student routes and hosts', () => {
    expect(PORTALS.STUDENT.defaultRoute).toBe('/home')
    expect(PORTALS.STUDENT.routes.agent).toBe('/agent')
    expect(PORTALS.STUDENT.routes.account).toBe('/me')
    expect(PORTALS.STUDENT.devHost).toBe('life.campus.local:5173')
    expect(PORTALS.STUDENT.prodHost).toBe('life.example.com')
  })

  it('configures merchant gates and scheme C routes', () => {
    expect(PORTALS.MERCHANT.defaultRoute).toBe('/home')
    expect(PORTALS.MERCHANT.routes.agent).toBe('/ai')
    expect(PORTALS.MERCHANT.gates).toEqual(['merchantVerified'])
    expect(PORTALS.MERCHANT.devHost).toBe('merchant.campus.local:5175')
  })

  it('configures admin dashboard and agent hub routes', () => {
    expect(PORTALS.ADMIN.defaultRoute).toBe('/dashboard')
    expect(PORTALS.ADMIN.routes.agent).toBe('/ai/agents')
    expect(PORTALS.ADMIN.routes.account).toBe('/profile/info')
    expect(PORTALS.ADMIN.prodHost).toBe('admin.example.com')
  })
})
