<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getDashboardVariant } from '../utils/roleNavigation'
import DashboardView from './DashboardView.vue'
import AdminDashboardView from './admin/AdminDashboardView.vue'

const auth = useAuthStore()
const dashboardComponent = computed(() =>
  getDashboardVariant(auth.user?.role) === 'admin'
    ? AdminDashboardView
    : DashboardView
)
</script>

<template>
  <component :is="dashboardComponent" />
</template>
