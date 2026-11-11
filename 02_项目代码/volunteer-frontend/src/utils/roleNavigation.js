const MENU_ITEMS = {
  dashboard: { key: 'dashboard', path: '/', label: '概览' },
  universities: { key: 'universities', path: '/universities', label: '院校查询' },
  recommendations: { key: 'recommendations', path: '/recommendations', label: '冲稳保筛选' },
  favorites: { key: 'favorites', path: '/favorites', label: '我的收藏' },
  profile: { key: 'profile', path: '/profile', label: '个人档案' },
  adminUniversities: {
    key: 'adminUniversities',
    path: '/admin/universities',
    label: '院校维护'
  },
  adminStatistics: {
    key: 'adminStatistics',
    path: '/admin/statistics',
    label: '数据统计'
  }
}

export function getNavigationSections(role) {
  const isAdmin = role === 'ADMIN'
  const itemKeys = isAdmin
    ? ['dashboard', 'adminUniversities', 'adminStatistics']
    : ['dashboard', 'universities', 'recommendations', 'favorites', 'profile']
  return [{
    label: isAdmin ? '管理功能' : '学生服务',
    items: itemKeys.map((key) => MENU_ITEMS[key])
  }]
}

export function getDashboardVariant(role) {
  return role === 'ADMIN' ? 'admin' : 'student'
}
