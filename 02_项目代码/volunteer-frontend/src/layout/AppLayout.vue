<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataBoard,
  School,
  Setting,
  Star,
  SwitchButton,
  TrendCharts,
  User
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

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
        <el-menu-item index="/">
          <el-icon><DataBoard /></el-icon>
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="/universities">
          <el-icon><School /></el-icon>
          <span>院校查询</span>
        </el-menu-item>
        <el-menu-item index="/recommendations">
          <el-icon><TrendCharts /></el-icon>
          <span>冲稳保筛选</span>
        </el-menu-item>
        <el-menu-item index="/favorites">
          <el-icon><Star /></el-icon>
          <span>我的收藏</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人档案</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/admin/universities">
          <el-icon><Setting /></el-icon>
          <span>院校维护</span>
        </el-menu-item>
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
          <h1>{{ route.meta.title || '志愿填报辅助系统' }}</h1>
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
