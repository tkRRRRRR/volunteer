import { describe, expect, it } from 'vitest'
import {
  getDashboardVariant,
  getNavigationSections
} from './roleNavigation'

describe('role based navigation', () => {
  it('gives students the complete volunteer workflow without admin tools', () => {
    const sections = getNavigationSections('STUDENT')
    const keys = sections.flatMap((section) => section.items.map((item) => item.key))

    expect(keys).toEqual([
      'dashboard',
      'universities',
      'recommendations',
      'favorites',
      'profile'
    ])
    expect(keys).not.toContain('adminUniversities')
    expect(getDashboardVariant('STUDENT')).toBe('student')
  })

  it('gives administrators management tools without recommendations or favorites', () => {
    const sections = getNavigationSections('ADMIN')
    const keys = sections.flatMap((section) => section.items.map((item) => item.key))

    expect(keys).toEqual(['dashboard', 'adminUniversities', 'adminStatistics'])
    expect(keys).not.toContain('recommendations')
    expect(keys).not.toContain('favorites')
    expect(getDashboardVariant('ADMIN')).toBe('admin')
  })
})
