import { describe, expect, it } from 'vitest'
import {
  buildAdmissionPayload,
  buildAdmissionQuery,
  buildMajorPayload,
  canCreateAdmissionRecord,
  createEmptyAdmissionForm,
  formatFileSize,
  validateImportFile
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

  it('only enables admission creation when the university has majors', () => {
    expect(canCreateAdmissionRecord([])).toBe(false)
    expect(canCreateAdmissionRecord([{ id: 1, name: '软件工程' }])).toBe(true)
  })

  it('creates a traceable empty admission form with a non-official source', () => {
    expect(createEmptyAdmissionForm(2026)).toEqual({
      majorId: null,
      province: '',
      year: 2026,
      subjectTrack: '',
      batch: '',
      minScore: null,
      minRank: null,
      planCount: null,
      source: '演示数据（非官方）'
    })
  })

  it('accepts CSV and Excel files within the size limit', () => {
    expect(validateImportFile({ name: 'records.csv', size: 1024 }).valid).toBe(true)
    expect(validateImportFile({ name: 'records.XLSX', size: 1024 }).valid).toBe(true)
    expect(validateImportFile({ name: 'records.xls', size: 2 * 1024 * 1024 }).valid).toBe(true)
  })

  it('rejects unsupported files and files over 2 MB', () => {
    expect(validateImportFile({ name: 'records.xlsm', size: 1024 }).message)
      .toBe('仅支持 CSV、XLSX 和 XLS 文件')
    expect(validateImportFile({
      name: 'records.csv',
      size: 2 * 1024 * 1024 + 1
    }).message).toBe('文件不能超过 2 MB')
  })

  it('formats file sizes for display', () => {
    expect(formatFileSize(512)).toBe('512 B')
    expect(formatFileSize(1536)).toBe('1.5 KB')
    expect(formatFileSize(2 * 1024 * 1024)).toBe('2 MB')
  })
})
