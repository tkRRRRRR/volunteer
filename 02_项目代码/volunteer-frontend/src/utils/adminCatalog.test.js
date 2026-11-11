import { describe, expect, it } from 'vitest'
import {
  buildAdmissionPayload,
  buildAdmissionQuery,
  buildMajorPayload
} from './adminCatalog'

describe('admin catalog form helpers', () => {
  it('trims major text fields', () => {
    expect(buildMajorPayload({
      name: ' 软件工程 ',
      category: ' 工学 ',
      subjectRequirement: ' 物理+不限 ',
      description: ' 演示专业 '
    })).toEqual({
      name: '软件工程',
      category: '工学',
      subjectRequirement: '物理+不限',
      description: '演示专业'
    })
  })

  it('normalizes admission fields and optional plan count', () => {
    expect(buildAdmissionPayload({
      majorId: '1',
      province: ' 山西 ',
      year: '2025',
      subjectTrack: ' 物理类 ',
      batch: ' 本科批 ',
      minScore: '578',
      minRank: '9600',
      planCount: '',
      source: ' 演示数据（非官方） '
    })).toEqual({
      majorId: 1,
      province: '山西',
      year: 2025,
      subjectTrack: '物理类',
      batch: '本科批',
      minScore: 578,
      minRank: 9600,
      planCount: null,
      source: '演示数据（非官方）'
    })
  })

  it('removes empty admission filters and normalizes pagination', () => {
    expect(buildAdmissionQuery({
      page: '2',
      size: '20',
      majorId: '',
      year: null,
      province: ' 山西 ',
      subjectTrack: undefined
    })).toEqual({
      page: 2,
      size: 20,
      province: '山西'
    })
  })

  it('keeps a selected major and year in admission filters', () => {
    expect(buildAdmissionQuery({
      page: 1,
      size: 10,
      majorId: '3',
      year: '2025'
    })).toEqual({
      page: 1,
      size: 10,
      majorId: 3,
      year: 2025
    })
  })
})
