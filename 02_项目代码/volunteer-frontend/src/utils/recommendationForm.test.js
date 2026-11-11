import { describe, expect, it } from 'vitest'
import {
  buildEmptyRecommendationOption,
  buildRecommendationPayload
} from './recommendationForm'

describe('recommendation form helpers', () => {
  it('sends only the rank when rank mode is selected', () => {
    expect(buildRecommendationPayload({
      province: '山西',
      examYear: 2026,
      subjectTrack: '物理类',
      queryMode: 'rank',
      score: 560,
      provinceRank: 10000,
      preferredProvince: '',
      universityLevel: '',
      keyword: ''
    })).toMatchObject({
      score: null,
      provinceRank: 10000
    })
  })

  it('sends only the score when score mode is selected', () => {
    expect(buildRecommendationPayload({
      province: '山西',
      examYear: 2026,
      subjectTrack: '历史类',
      queryMode: 'score',
      score: 540,
      provinceRank: 20000,
      preferredProvince: '江苏',
      universityLevel: '双一流',
      keyword: ' 法学 '
    })).toMatchObject({
      score: 540,
      provinceRank: null,
      keyword: '法学'
    })
  })

  it('builds a text empty state for the result chart', () => {
    const option = buildEmptyRecommendationOption('请先设置筛选条件')

    expect(option.graphic[0].type).toBe('text')
    expect(option.graphic[0].style.text).toBe('请先设置筛选条件')
    expect(option.series).toEqual([])
  })
})
