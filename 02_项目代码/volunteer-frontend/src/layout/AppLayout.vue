<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataBoard,
  DataAnalysis,
  School,
  Setting,
  Star,
  SwitchButton,
  TrendCharts,
  User
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { getNavigationSections } from '../utils/roleNavigation'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => {
  if (route.meta.title === '工作台概览' && auth.isAdmin) {
    return '管理概览'
  }
  return route.meta.title || '志愿填报辅助系统'
})
const navigationSections = computed(() =>
  getNavigationSections(auth.user?.role)
)
const menuIcons = {
  dashboard: DataBoard,
  universities: School,
  recommendations: TrendCharts,
  favorites: Star,
  profile: User,
  adminUniversities: Setting,
  adminStatistics: DataAnalysis
}

const logout = () => {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">志</div>
        <div>
          <strong>志愿导航</strong>
          <span>录取数据辅助筛选</span>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="side-menu">
        <template v-for="section in navigationSections" :key="section.label">
          <div class="menu-section-label">{{ section.label }}</div>
          <el-menu-item
            v-for="item in section.items"
            :key="item.key"
            :index="item.path"
          >
            <el-icon><component :is="menuIcons[item.key]" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-avatar">{{ auth.displayName.slice(0, 1) }}</div>
        <div class="user-copy">
          <strong>{{ auth.displayName }}</strong>
          <span>{{ auth.isAdmin ? '管理员' : '学生账号' }}</span>
        </div>
        <el-button text circle title="退出登录" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div>
          <span class="eyebrow">GAOKAO VOLUNTEER ASSISTANT</span>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="topbar-note">
          历史数据仅作辅助参考，不构成录取承诺
        </div>
      </header>
      <section class="page-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<style scoped>
.menu-section-label {
  padding: 12px 20px 8px;
  color: #70849a;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}
</style>
