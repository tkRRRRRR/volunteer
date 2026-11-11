export function canCompareFavorites(selectionCount) {
  return selectionCount >= 2 && selectionCount <= 4
}

// 收藏项只依赖公开院校详情中的录取点，失败项保留原收藏信息并单独标记。
export function buildComparisonRows(selectedFavorites, detailByUniversityId) {
  return selectedFavorites.map((favorite) => {
    const detail = detailByUniversityId.get(favorite.universityId)
    if (!detail) {
      return {
        ...favorite,
        latestYear: null,
        minScore: null,
        minRank: null,
        source: '',
        loadError: true
      }
    }

    // 整校收藏没有具体专业，不能从多个专业中随意挑一条录取数据。
    const points = [...(detail.admissionPoints || [])]
      .filter((point) => favorite.majorId && point.majorId === favorite.majorId)
      .sort((left, right) => right.year - left.year)
    const latest = points[0]
    return {
      ...favorite,
      latestYear: latest?.year ?? null,
      minScore: latest?.minScore ?? null,
      minRank: latest?.minRank ?? null,
      source: latest?.source || '',
      loadError: false
    }
  })
}
