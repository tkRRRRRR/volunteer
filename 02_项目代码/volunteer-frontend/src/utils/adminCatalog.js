const trimText = (value) => (typeof value === 'string' ? value.trim() : value)

const toNumberOrNull = (value) => {
  if (value === '' || value === null || value === undefined) {
    return null
  }
  const normalized = Number(value)
  return Number.isFinite(normalized) ? normalized : null
}

const toPositiveInteger = (value, fallback) => {
  const normalized = Number.parseInt(value, 10)
  return Number.isFinite(normalized) && normalized > 0 ? normalized : fallback
}

export function buildMajorPayload(form) {
  return {
    name: trimText(form.name)?.trim() || '',
    category: trimText(form.category)?.trim() || '',
    subjectRequirement: trimText(form.subjectRequirement)?.trim() || '',
    description: trimText(form.description)?.trim() || ''
  }
}

export function buildAdmissionPayload(form) {
  return {
    majorId: toNumberOrNull(form.majorId),
    province: trimText(form.province)?.trim() || '',
    year: toNumberOrNull(form.year),
    subjectTrack: trimText(form.subjectTrack)?.trim() || '',
    batch: trimText(form.batch)?.trim() || '',
    minScore: toNumberOrNull(form.minScore),
    minRank: toNumberOrNull(form.minRank),
    planCount: toNumberOrNull(form.planCount),
    source: trimText(form.source)?.trim() || ''
  }
}

export function buildAdmissionQuery(filters) {
  const query = {
    page: toPositiveInteger(filters.page, 1),
    size: toPositiveInteger(filters.size, 10)
  }
  const majorId = toNumberOrNull(filters.majorId)
  const year = toNumberOrNull(filters.year)
  const province = trimText(filters.province)?.trim()
  const subjectTrack = trimText(filters.subjectTrack)?.trim()

  if (majorId !== null) query.majorId = majorId
  if (year !== null) query.year = year
  if (province) query.province = province
  if (subjectTrack) query.subjectTrack = subjectTrack
  return query
}

export function canCreateAdmissionRecord(majors) {
  return Array.isArray(majors) && majors.length > 0
}

export function createEmptyAdmissionForm(year) {
  return {
    majorId: null,
    province: '',
    year,
    subjectTrack: '',
    batch: '',
    minScore: null,
    minRank: null,
    planCount: null,
    source: '演示数据（非官方）'
  }
}

export function validateImportFile(file) {
  const extension = file?.name?.split('.').pop()?.toLowerCase()
  if (!['csv', 'xlsx', 'xls'].includes(extension)) {
    return { valid: false, message: '仅支持 CSV、XLSX 和 XLS 文件' }
  }
  if (Number(file.size) > 2 * 1024 * 1024) {
    return { valid: false, message: '文件不能超过 2 MB' }
  }
  return { valid: true, message: '' }
}

export function formatFileSize(bytes) {
  const size = Number(bytes) || 0
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${Number((size / 1024).toFixed(1))} KB`
  }
  return `${Number((size / (1024 * 1024)).toFixed(1))} MB`
}

export function isExcelContentType(contentType) {
  return String(contentType || '')
    .toLowerCase()
    .includes('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
}
