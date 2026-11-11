<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { getAdminStatistics, getAdminStudents } from '../../api/adminStatistics'
import {
  buildCompletenessItems,
  buildDistributionOption,
  buildFavoriteRows,
  buildYearTrendOption
} from '../../utils/adminStatistics'

const overview = ref(null)
const overviewLoading = ref(false)
const overviewError = ref('')
const studentLoading = ref(false)
const studentError = ref('')
const students = ref([])
const studentTotal = ref(0)
const levelChartRef = ref()
const provinceChartRef = ref()
const trendChartRef = ref()
const charts = {}
const studentQuery = reactive({
  keyword: '',
  page: 1,
  size: 10
})

const metrics = computed(() => [
  { label: '院校数据', value: overview.value?.universityCount || 0, tone: 'blue' },
  { label: '专业数据', value: overview.value?.majorCount || 0, tone: 'cyan' },
  { label: '录取记录', value: overview.value?.admissionRecordCount || 0, tone: 'gold' },
  { label: '学生账号', value: overview.value?.studentCount || 0, tone: 'green' },
  { label: '收藏记录', value: overview.value?.favoriteCount || 0, tone: 'rose' }
])

const favoriteRows = computed(() =>
  buildFavoriteRows(overview.value?.topFavorites)
)

const completenessItems = computed(() =>
  buildCompletenessItems(overview.value?.dataCompleteness)
)

const renderChart = (key, element, option) => {
  if (!element) return
  if (!charts[key]) {
    charts[key] = echarts.init(element)
  }
  charts[key].setOption(option, true)
}

const renderCharts = () => {
  if (!overview.value) return
  renderChart(
    'levels',
    levelChartRef.value,
    buildDistributionOption(overview.value.universityLevels, '院校数量')
  )
  renderChart(
    'provinces',
    provinceChartRef.value,
    buildDistributionOption(overview.value.universityProvinces, '院校数量')
  )
  renderChart(
    'trends',
    trendChartRef.value,
    buildYearTrendOption(overview.value.admissionTrends)
  )
}

const resizeCharts = () => {
  Object.values(charts).forEach((chart) => chart.resize())
}

const loadOverview = async () => {
  overviewLoading.value = true
  overviewError.value = ''
  try {
    overview.value = await getAdminStatistics()
    await nextTick()
    renderCharts()
  } catch (error) {
    overviewError.value = error.message || '统计数据加载失败'
  } finally {
    overviewLoading.value = false
  }
}

const loadStudents = async () => {
  studentLoading.value = true
  studentError.value = ''
  try {
    const page = await getAdminStudents({
      keyword: studentQuery.keyword.trim() || undefined,
      page: studentQuery.page,
      size: studentQuery.size
    })
    students.value = page.records
    studentTotal.value = page.total
  } catch (error) {
    students.value = []
    studentTotal.value = 0
    studentError.value = error.message || '学生账号加载失败'
  } finally {
    studentLoading.value = false
  }
}

const searchStudents = () => {
  studentQuery.page = 1
  loadStudents()
}

const refreshAll = async () => {
  await Promise.all([loadOverview(), loadStudents()])
}

onMounted(async () => {
  window.addEventListener('resize', resizeCharts)
  await refreshAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  Object.values(charts).forEach((chart) => chart.dispose())
})
</script>

<template>
  <div v-loading="overviewLoading" class="statistics-page">
    <div class="page-heading">
      <div>
        <h2>数据统计与学生账号</h2>
        <p>查看院校数据规模、录取趋势、收藏热度和数据完整情况。</p>
      </div>
      <el-button @click="refreshAll">刷新数据</el-button>
    </div>

    <el-alert
      v-if="overviewError"
      :title="overviewError"
      type="error"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button link type="primary" @click="loadOverview">重新加载</el-button>
      </template>
    </el-alert>

    <section class="metric-grid">
      <article
        v-for="metric in metrics"
        :key="metric.label"
        :class="['metric-card', metric.tone]"
      >
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </article>
    </section>

    <section class="chart-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <h3>院校层次分布</h3>
            <p>按院校层次统计当前数据量</p>
          </div>
        </div>
        <div ref="levelChartRef" class="chart-box" />
      </article>
      <article class="panel">
        <div class="panel-heading">
          <div>
            <h3>院校地区分布</h3>
            <p>按院校所在省份统计当前数据量</p>
          </div>
        </div>
        <div ref="provinceChartRef" class="chart-box" />
      </article>
    </section>

    <article class="panel">
      <div class="panel-heading">
        <div>
          <h3>历年录取趋势</h3>
          <p>记录数量、平均最低分和平均最低位次按年份展示</p>
        </div>
      </div>
      <div ref="trendChartRef" class="trend-chart" />
    </article>

    <section class="insight-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <h3>收藏院校 Top 5</h3>
            <p>按当前学生收藏记录聚合</p>
          </div>
        </div>
        <el-table
          :data="favoriteRows"
          empty-text="暂无收藏数据"
          stripe
        >
          <el-table-column prop="rank" label="排名" width="70" />
          <el-table-column prop="universityName" label="院校" min-width="180" />
          <el-table-column prop="favoriteCount" label="收藏数" width="90" />
        </el-table>
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <h3>数据完整度</h3>
            <p>提示需要继续补充的数据</p>
          </div>
        </div>
        <div class="completeness-list">
          <div
            v-for="item in completenessItems"
            :key="item.label"
            :class="['completeness-item', item.status]"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
            <el-tag
              :type="item.status === 'warning' ? 'warning' : 'success'"
              effect="plain"
            >
              {{ item.status === 'warning' ? '待完善' : '完整' }}
            </el-tag>
          </div>
        </div>
      </article>
    </section>

    <article class="panel student-panel">
      <div class="panel-heading student-heading">
        <div>
          <h3>学生账号</h3>
          <p>仅展示非敏感档案字段，不包含密码哈希。</p>
        </div>
        <div class="student-filter">
          <el-input
            v-model="studentQuery.keyword"
            clearable
            placeholder="用户名或昵称"
            style="width: 230px"
            @keyup.enter="searchStudents"
          />
          <el-button type="primary" @click="searchStudents">查询</el-button>
        </div>
      </div>

      <el-alert
        v-if="studentError"
        :title="studentError"
        type="error"
        :closable="false"
        show-icon
      >
        <template #default>
          <el-button link type="primary" @click="loadStudents">重新加载</el-button>
        </template>
      </el-alert>

      <el-table
        v-loading="studentLoading"
        :data="students"
        empty-text="暂无学生账号"
        stripe
      >
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="province" label="省份" width="90" />
        <el-table-column prop="examYear" label="高考年份" width="100" />
        <el-table-column prop="subjectTrack" label="科类" width="100" />
        <el-table-column prop="score" label="分数" width="85" />
        <el-table-column prop="provinceRank" label="位次" width="100" />
        <el-table-column prop="createdAt" label="注册时间" min-width="170" />
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="studentQuery.page"
          layout="total, prev, pager, next"
          :page-size="studentQuery.size"
          :total="studentTotal"
          @current-change="loadStudents"
        />
      </div>
    </article>
  </div>
</template>

<style scoped>
.statistics-page {
  display: grid;
  gap: 22px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
}

.metric-card span {
  color: var(--muted);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin-top: 12px;
  font-size: 30px;
  letter-spacing: -0.04em;
}

.metric-card.blue strong { color: #1f4e79; }
.metric-card.cyan strong { color: #317f8a; }
.metric-card.gold strong { color: #a97726; }
.metric-card.green strong { color: #2f7d59; }
.metric-card.rose strong { color: #a85661; }

.chart-grid,
.insight-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.panel {
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fff;
}

.panel-heading {
  margin-bottom: 16px;
}

.panel-heading h3 {
  margin: 0;
  font-size: 16px;
}

.panel-heading p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.chart-box {
  height: 290px;
}

.trend-chart {
  height: 330px;
}

.completeness-list {
  display: grid;
  gap: 10px;
}

.completeness-item {
  display: grid;
  grid-template-columns: 1fr 52px auto;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 11px;
  background: #f8fafc;
}

.completeness-item strong {
  font-size: 21px;
  text-align: right;
}

.completeness-item.warning strong {
  color: #b7791f;
}

.completeness-item.success strong {
  color: #2f7d59;
}

.student-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.student-filter {
  display: flex;
  gap: 10px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
}

:deep(.el-alert) {
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .chart-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
