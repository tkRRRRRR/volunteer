<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAdminStatistics } from '../../api/adminStatistics'
import { useAuthStore } from '../../stores/auth'
import { buildCompletenessItems } from '../../utils/adminStatistics'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const overview = ref(null)

const metrics = computed(() => [
  { label: '院校数据', value: overview.value?.universityCount || 0 },
  { label: '专业数据', value: overview.value?.majorCount || 0 },
  { label: '录取记录', value: overview.value?.admissionRecordCount || 0 },
  { label: '学生账号', value: overview.value?.studentCount || 0 }
])

const completenessItems = computed(() =>
  buildCompletenessItems(overview.value?.dataCompleteness)
)

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    overview.value = await getAdminStatistics()
  } catch (error) {
    errorMessage.value = error.message || '管理概览加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="admin-dashboard">
    <section class="admin-hero">
      <div>
        <span class="hero-kicker">管理员工作台</span>
        <h2>{{ auth.displayName }}，集中维护数据并检查系统完整度</h2>
        <p>管理端只保留院校数据维护和数据统计。学生成绩、冲稳保筛选与收藏属于学生工作台，不再混入管理员导航。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/admin/statistics')">
            打开数据统计
          </el-button>
          <el-button size="large" @click="router.push('/admin/universities')">
            维护院校数据
          </el-button>
        </div>
      </div>
      <div class="admin-badge">
        <span>当前角色</span>
        <strong>系统管理员</strong>
        <small>院校 · 专业 · 录取数据</small>
      </div>
    </section>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button link type="primary" @click="load">重新加载</el-button>
      </template>
    </el-alert>

    <section class="admin-metrics">
      <article v-for="metric in metrics" :key="metric.label">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </article>
    </section>

    <section class="admin-grid">
      <article class="admin-panel">
        <div class="panel-heading">
          <h3>数据处理提醒</h3>
          <p>优先补齐会影响学生查询和推荐的缺失数据。</p>
        </div>
        <div class="health-list">
          <div v-for="item in completenessItems" :key="item.label">
            <span>{{ item.label }}</span>
            <strong :class="item.status">{{ item.count }}</strong>
          </div>
        </div>
      </article>

      <article class="admin-panel">
        <div class="panel-heading">
          <h3>常用操作</h3>
          <p>进入对应模块完成日常维护。</p>
        </div>
        <button class="action-row" @click="router.push('/admin/universities')">
          <div>
            <strong>维护院校、专业与录取记录</strong>
            <small>新增、编辑、删除和批量导入</small>
          </div>
          <span>进入</span>
        </button>
        <button class="action-row" @click="router.push('/admin/statistics')">
          <div>
            <strong>查看统计与学生账号</strong>
            <small>分布、趋势、排行和完整度</small>
          </div>
          <span>查看</span>
        </button>
      </article>
    </section>
  </div>
</template>

<style scoped>
.admin-dashboard {
  display: grid;
  gap: 22px;
}

.admin-hero {
  min-height: 300px;
  padding: 40px;
  border-radius: 20px;
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(260px, 0.5fr);
  align-items: center;
  gap: 36px;
  color: #fff;
  background:
    radial-gradient(circle at 88% 18%, rgba(231, 191, 116, 0.2), transparent 28%),
    linear-gradient(125deg, #132b40, #1f4e79);
  box-shadow: 0 20px 50px rgba(20, 43, 64, 0.18);
}

.hero-kicker {
  color: #e7bf74;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.admin-hero h2 {
  max-width: 720px;
  margin: 16px 0;
  font-size: 34px;
  line-height: 1.3;
  letter-spacing: -0.04em;
}

.admin-hero p {
  max-width: 720px;
  margin: 0;
  color: #c3d2df;
  line-height: 1.8;
  font-size: 13px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.admin-badge {
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.admin-badge span,
.admin-badge strong,
.admin-badge small {
  display: block;
}

.admin-badge span {
  color: #9fb4c6;
  font-size: 12px;
}

.admin-badge strong {
  margin: 14px 0 8px;
  font-size: 25px;
}

.admin-badge small {
  color: #c3d2df;
}

.admin-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.admin-metrics article,
.admin-panel {
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
}

.admin-metrics span {
  color: var(--muted);
  font-size: 12px;
}

.admin-metrics strong {
  display: block;
  margin-top: 10px;
  color: var(--brand);
  font-size: 28px;
}

.admin-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.panel-heading h3 {
  margin: 0;
  font-size: 16px;
}

.panel-heading p {
  margin: 6px 0 18px;
  color: var(--muted);
  font-size: 12px;
}

.health-list {
  display: grid;
  gap: 10px;
}

.health-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 15px;
  border-radius: 10px;
  background: #f8fafc;
}

.health-list strong.warning { color: #b7791f; }
.health-list strong.success { color: #2f7d59; }

.action-row {
  width: 100%;
  margin-bottom: 10px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--ink);
  text-align: left;
  background: #f9fbfd;
  cursor: pointer;
}

.action-row strong,
.action-row small {
  display: block;
}

.action-row small {
  margin-top: 6px;
  color: var(--muted);
}

.action-row span {
  color: var(--brand);
}
</style>
