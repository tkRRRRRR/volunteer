import { describe, expect, it } from 'vitest'
import {
  buildCompletenessItems,
  buildDistributionOption,
  buildFavoriteRows,
  buildYearTrendOption
} from './adminStatistics'

describe('admin statistics chart helpers', () => {
  it('builds a distribution chart from statistics items', () => {
    const option = buildDistributionOption([
      { label: '本科', count: 3 },
      { label: '专科', count: 1 }
    ], '院校数量')

    expect(option.xAxis.data).toEqual(['本科', '专科'])
    expect(option.series[0].data).toEqual([3, 1])
  })

  it('builds record, score and rank trend series in year order', () => {
    const option = buildYearTrendOption([
      { year: 2024, recordCount: 4, averageMinScore: 560, averageMinRank: 12000 },
      { year: 2025, recordCount: 6, averageMinScore: 570, averageMinRank: 11000 }
    ])

    expect(option.xAxis.data).toEqual([2024, 2025])
    expect(option.series.map((item) => item.data)).toEqual([
      [4, 6],
      [560, 570],
      [12000, 11000]
    ])
  })

  it('returns visible empty states for empty charts and favorite lists', () => {
    expect(buildDistributionOption([], '院校数量').graphic[0].type).toBe('text')
    expect(buildYearTrendOption([]).graphic[0].type).toBe('text')
    expect(buildFavoriteRows([])).toEqual([])
  })

  it('describes data completeness with warning and success states', () => {
    expect(buildCompletenessItems({
      universitiesWithoutMajors: 2,
      universitiesWithoutAdmissions: 0,
      majorsWithoutAdmissions: 1
    })).toEqual([
      { label: '缺少专业的院校', count: 2, status: 'warning' },
      { label: '缺少录取记录的院校', count: 0, status: 'success' },
      { label: '缺少录取记录的专业', count: 1, status: 'warning' }
    ])
  })
})
