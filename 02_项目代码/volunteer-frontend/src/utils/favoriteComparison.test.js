import { describe, expect, it } from 'vitest'
import { buildComparisonRows, canCompareFavorites } from './favoriteComparison'

const favorites = [
  {
    id: 1,
    universityId: 1,
    universityName: '华北示例大学',
    majorId: 1,
    majorName: '软件工程',
    province: '山西',
    city: '太原',
    universityLevel: '本科'
  },
  {
    id: 2,
    universityId: 2,
    universityName: '滨河理工大学',
    majorId: 3,
    majorName: '计算机科学与技术',
    province: '山西',
    city: '大同',
    universityLevel: '本科'
  },
  {
    id: 3,
    universityId: 3,
    universityName: '江南示例大学',
    majorId: 5,
    majorName: '软件工程',
    province: '江苏',
    city: '南京',
    universityLevel: '本科'
  }
]

const detailMap = new Map([
  [1, {
    admissionPoints: [
      { majorId: 1, year: 2024, minScore: 572, minRank: 10100, source: '演示数据（非官方）' },
      { majorId: 1, year: 2025, minScore: 578, minRank: 9600, source: '演示数据（非官方）' }
    ]
  }],
  [3, {
    admissionPoints: [
      { majorId: 5, year: 2025, minScore: 551, minRank: 16500, source: '演示数据（非官方）' }
    ]
  }]
])

describe('favorite comparison', () => {
  it.each([[0, false], [1, false], [2, true], [4, true], [5, false]])(
    'validates comparison selection count %i',
    (count, expected) => expect(canCompareFavorites(count)).toBe(expected)
  )

  it('keeps unavailable detail items while rendering successful comparisons', () => {
    const rows = buildComparisonRows(favorites, detailMap)

    expect(rows).toHaveLength(3)
    expect(rows.find((row) => row.universityId === 2)?.loadError).toBe(true)
  })

  it('uses the latest admission point for the selected major', () => {
    const rows = buildComparisonRows(favorites, detailMap)

    expect(rows[0].latestYear).toBe(2025)
    expect(rows[0].minScore).toBe(578)
    expect(rows[0].minRank).toBe(9600)
  })

  it('does not use an arbitrary major point for a university-level favorite', () => {
    const universityFavorite = {
      ...favorites[0],
      id: 4,
      majorId: null,
      majorName: '院校专业组'
    }

    const rows = buildComparisonRows([universityFavorite], detailMap)

    expect(rows[0].latestYear).toBeNull()
    expect(rows[0].minScore).toBeNull()
    expect(rows[0].minRank).toBeNull()
  })
})
