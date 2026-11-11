import { describe, expect, it } from 'vitest'
import { buildAdmissionTrendOption } from './universityChart'

const points = [
  { majorId: 1, majorName: '软件工程', year: 2023, minScore: 565, minRank: 10800 },
  { majorId: 1, majorName: '软件工程', year: 2024, minScore: 572, minRank: 10100 },
  { majorId: 1, majorName: '软件工程', year: 2025, minScore: 578, minRank: 9600 },
  { majorId: 2, majorName: '数据科学与大数据技术', year: 2025, minScore: 570, minRank: 10400 }
]

describe('buildAdmissionTrendOption', () => {
  it('builds three-year score and rank series for one major', () => {
    const option = buildAdmissionTrendOption(points, 1)

    expect(option.series[0].data).toEqual([565, 572, 578])
    expect(option.series[1].data).toEqual([10800, 10100, 9600])
  })

  it('returns an empty-state option when the selected major has no points', () => {
    const option = buildAdmissionTrendOption(points, 999)

    expect(option.graphic[0].type).toBe('text')
  })

  it('filters points from other majors', () => {
    const option = buildAdmissionTrendOption(points, 1)

    expect(option.series[0].data).toHaveLength(3)
  })
})
