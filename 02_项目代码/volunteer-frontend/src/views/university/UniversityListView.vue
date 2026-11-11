<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getUniversities, getUniversityOverview } from '../../api/university'
import { addFavorite } from '../../api/favorite'
import { useAuthStore } from '../../stores/auth'
import { buildAdmissionTrendOption } from '../../utils/universityChart'

const auth = useAuthStore()
const loading = ref(false)
const universities = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const selected = ref(null)
const detail = ref(null)
const detailLoading = ref(false)
const detailError = ref('')
const selectedMajorId = ref(null)
const chartRef = ref()
let chart
const query = reactive({
  keyword: '',
  province: '',
  level: '',
  page: 1,
  size: 10
})

const load = async () => {
  loading.value = true
  try {
    const page = await getUniversities(query)
    universities.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  Object.assign(query, { keyword: '', province: '', level: '', page: 1 })
  load()
}

const openDetail = async (row) => {
  selected.value = row
  detail.value = null
  detailError.value = ''
  drawerVisible.value = true
  await loadDetail(row.id)
}

const loadDetail = async (universityId) => {
  detailLoading.value = true
  detailError.value = ''
  try {
    detail.value = await getUniversityOverview(universityId, {
      province: auth.user?.province || undefined,
      subjectTrack: auth.user?.subjectTrack || undefined
    })
    selectedMajorId.value = detail.value.majors?.[0]?.id || null
    await nextTick()
    renderChart()
  } catch {
    detailError.value = '院校详情加载失败，请稍后重试'
  } finally {
    detailLoading.value = false
  }
}

const renderChart = async () => {
  await nextTick()
  if (!chartRef.value || !detail.value) return
  chart ||= echarts.init(chartRef.value)
  chart.setOption(
    buildAdmissionTrendOption(detail.value.admissionPoints, selectedMajorId.value),
    true
  )
  chart.resize()
}

const closeDetail = () => {
  chart?.dispose()
  chart = null
}

const resize = () => chart?.resize()

const favorite = async (row) => {
  await addFavorite({ universityId: row.id, majorId: null })
  ElMessage.success('已加入收藏')
}

watch(selectedMajorId, renderChart)

onMounted(load)
onMounted(() => window.addEventListener('resize', resize))
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  closeDetail()
})
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div>
        <h2>院校查询</h2>
        <p>院校名称来自公开院校信息，录取分数和位次为本系统生成的演示数据。</p>
      </div>
      <el-tag type="warning" effect="plain">演示录取数据</el-tag>
    </div>

    <div class="filter-row">
      <el-input v-model="query.keyword" clearable placeholder="院校名称、标签或专业方向" style="width: 260px" @keyup.enter="load" />
      <el-select v-model="query.province" clearable placeholder="所在省份" style="width: 150px">
        <el-option label="山西" value="山西" />
        <el-option label="北京" value="北京" />
        <el-option label="天津" value="天津" />
        <el-option label="辽宁" value="辽宁" />
        <el-option label="吉林" value="吉林" />
        <el-option label="黑龙江" value="黑龙江" />
        <el-option label="上海" value="上海" />
        <el-option label="江苏" value="江苏" />
        <el-option label="浙江" value="浙江" />
        <el-option label="安徽" value="安徽" />
        <el-option label="福建" value="福建" />
        <el-option label="山东" value="山东" />
        <el-option label="河南" value="河南" />
        <el-option label="湖北" value="湖北" />
        <el-option label="湖南" value="湖南" />
        <el-option label="广东" value="广东" />
        <el-option label="四川" value="四川" />
        <el-option label="重庆" value="重庆" />
        <el-option label="陕西" value="陕西" />
        <el-option label="甘肃" value="甘肃" />
        <el-option label="新疆" value="新疆" />
        <el-option label="内蒙古" value="内蒙古" />
        <el-option label="江西" value="江西" />
        <el-option label="广西" value="广西" />
        <el-option label="云南" value="云南" />
        <el-option label="贵州" value="贵州" />
        <el-option label="海南" value="海南" />
      </el-select>
      <el-select v-model="query.level" clearable placeholder="院校层次" style="width: 150px">
        <el-option label="双一流" value="双一流" />
        <el-option label="省重点" value="省重点" />
        <el-option label="普通本科" value="普通本科" />
      </el-select>
      <el-button type="primary" @click="query.page = 1; load()">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="universities" stripe>
      <el-table-column prop="name" label="院校" min-width="190">
        <template #default="{ row }">
          <div class="university-name">
            <strong>{{ row.name }}</strong>
            <span>{{ row.tags }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="province" label="省份" width="90" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="level" label="层次" width="90">
        <template #default="{ row }"><el-tag effect="plain">{{ row.level }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="description" label="简介" min-width="260" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link @click="favorite(row)">收藏</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="院校详情" size="560px" @closed="closeDetail">
      <div v-loading="detailLoading" class="detail-body">
        <el-result
          v-if="detailError"
          icon="error"
          title="详情加载失败"
          :sub-title="detailError"
        >
          <template #extra>
            <el-button type="primary" @click="loadDetail(selected.id)">重新加载</el-button>
          </template>
        </el-result>

        <template v-else-if="detail">
        <div class="detail-title">
          <span>院校档案</span>
          <h3>{{ detail.university.name }}</h3>
          <p>{{ detail.university.province }} · {{ detail.university.city }} · {{ detail.university.level }} · {{ detail.university.type }}</p>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="院校标签">{{ detail.university.tags }}</el-descriptions-item>
          <el-descriptions-item label="院校简介">{{ detail.university.description }}</el-descriptions-item>
          <el-descriptions-item label="官网">{{ detail.university.website }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <h4>开设专业</h4>
          <el-table v-if="detail.majors.length" :data="detail.majors" size="small" stripe>
            <el-table-column prop="name" label="专业" min-width="170" />
            <el-table-column prop="category" label="门类" width="90" />
            <el-table-column prop="subjectRequirement" label="选科要求" min-width="130" />
          </el-table>
          <el-empty v-else description="该院校暂无专业数据" :image-size="72" />
        </div>

        <div v-if="detail.majors.length" class="detail-section">
          <div class="chart-heading">
            <h4>历年录取趋势</h4>
            <el-select v-model="selectedMajorId" size="small" style="width: 190px">
              <el-option
                v-for="major in detail.majors"
                :key="major.id"
                :label="major.name"
                :value="major.id"
              />
            </el-select>
          </div>
          <div v-if="detail.admissionPoints.length" ref="chartRef" class="trend-chart"></div>
          <el-empty v-else description="该院校暂无可展示的录取数据" :image-size="72" />
        </div>

        <el-alert
          class="detail-alert"
          title="页面中的分数和位次为演示数据，请以学校官网和省级考试院公布的信息为准。"
          type="warning"
          :closable="false"
          show-icon
        />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.university-name strong,
.university-name span {
  display: block;
}

.university-name strong {
  font-size: 14px;
}

.university-name span {
  margin-top: 5px;
  color: var(--muted);
  font-size: 11px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}

.detail-title {
  padding: 4px 0 22px;
}

.detail-body {
  min-height: 360px;
}

.detail-section {
  margin-top: 24px;
}

.detail-section h4 {
  margin: 0 0 12px;
  font-size: 16px;
}

.chart-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.chart-heading h4 {
  margin: 0;
}

.trend-chart {
  width: 100%;
  height: 290px;
}

.detail-title span {
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
}

.detail-title h3 {
  margin: 9px 0;
  font-size: 25px;
}

.detail-title p {
  margin: 0;
  color: var(--muted);
}

.detail-alert {
  margin-top: 20px;
}
</style>
