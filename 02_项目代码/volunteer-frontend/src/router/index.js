import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppLayout from '../layout/AppLayout.vue'
import LoginView from '../views/auth/LoginView.vue'
import DashboardEntryView from '../views/DashboardEntryView.vue'
import UniversityListView from '../views/university/UniversityListView.vue'
import RecommendationView from '../views/recommend/RecommendationView.vue'
import FavoriteView from '../views/user/FavoriteView.vue'
import ProfileView from '../views/user/ProfileView.vue'
import AdminUniversityView from '../views/admin/AdminUniversityView.vue'
import AdminStatisticsView from '../views/admin/AdminStatisticsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', name: 'dashboard', component: DashboardEntryView, meta: { title: '首页' } },
        { path: 'universities', name: 'universities', component: UniversityListView, meta: { roles: ['STUDENT'], title: '院校查询' } },
        { path: 'recommendations', name: 'recommendations', component: RecommendationView, meta: { roles: ['STUDENT'], title: '冲稳保筛选' } },
        { path: 'favorites', name: 'favorites', component: FavoriteView, meta: { roles: ['STUDENT'], title: '我的收藏' } },
        { path: 'profile', name: 'profile', component: ProfileView, meta: { roles: ['STUDENT'], title: '个人档案' } },
        {
          path: 'admin/universities',
          name: 'admin-universities',
          component: AdminUniversityView,
          meta: { roles: ['ADMIN'], title: '院校数据维护' }
        },
        {
          path: 'admin/statistics',
          name: 'admin-statistics',
          component: AdminStatisticsView,
          meta: { roles: ['ADMIN'], title: '数据统计' }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  // 未登录用户只能访问公开页面，登录后根据路由角色限制管理入口。
  if (!to.meta.public && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.token) {
    return { name: 'dashboard' }
  }
  if (to.meta.roles && !to.meta.roles.includes(auth.user?.role)) {
    return { name: 'dashboard' }
  }
})

export default router
