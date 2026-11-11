const trimText = (value) => (typeof value === 'string' ? value.trim() : value)

const toNumberOrNull = (value) => {
  if (value === '' || value === null || value === undefined) return null
  const normalized = Number(value)
  return Number.isFinite(normalized) ? normalized : null
}

export function buildRecommendationPayload(form) {
  const isScoreMode = form.queryMode === 'score'
  return {
    province: trimText(form.province)?.trim() || '',
    examYear: toNumberOrNull(form.examYear),
    subjectTrack: trimText(form.subjectTrack)?.trim() || '',
    score: isScoreMode ? toNumberOrNull(form.score) : null,
    provinceRank: isScoreMode ? null : toNumberOrNull(form.provinceRank),
    preferredProvince: trimText(form.preferredProvince)?.trim() || '',
    universityLevel: trimText(form.universityLevel)?.trim() || '',
    keyword: trimText(form.keyword)?.trim() || ''
  }
}

export function buildEmptyRecommendationOption(text) {
  return {
    graphic: [{
      type: 'text',
      left: 'center',
      top: 'middle',
      silent: true,
      style: {
        text,
        fill: '#94a3b8',
        fontSize: 14
      }
    }],
    series: []
  }
}
