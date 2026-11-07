<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { useAuthStore } from '../../stores/auth'
import { createRecommendation } from '../../api/recommendation'
import { addFavorite } from '../../api/favorite'

const auth = useAuthStore()
const loading = ref(false)
const results = ref([])
const chartRef = ref()
let chart

const form = reactive({
  province: auth.user?.province || '山西',
  examYear: auth.user?.examYear || 2026,
  subjectTrack: auth.user?.subjectTrack || '物理类',
  score: auth.user?.score || 560,
  provinceRank: auth.user?.provinceRank || 10000,
  preferredProvince: '',
  universityLevel: '',
  keyword: ''
})

const levelClass = (level) => ({
  保底: 'level-safe',
  稳妥: 'level-steady',
  冲刺: 'level-reach',
  不建议: 'level-risk'
}[level])

const renderChart = async () => {
  await nextTick()
  if (!chartRef.value) return
  chart ||= echarts.init(chartRef.value)
  const counts = ['保底', '稳妥', '冲刺', '不建议'].map((level) => ({
    name: level,
    value: results.value.filter((item) => item.matchLevel === level).length
  }))
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle' },
    series: [{
      type: 'pie',
      radius: ['48%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 3 },
      label: { formatter: '{b}\n{c} 条' },
      data: counts
    }],
    color: ['#4c9b68', '#4e83b5', '#d69b38', '#b95d5d']
  })
}

const submit = async () => {
  loading.value = true
  try {
    results.value = await createRecommendation(form)
    if (!results.value.length) {
      ElMessage.warning('当前条件下没有匹配记录，请调整筛选条件')
    }
    renderChart()
  } finally {
    loading.value = false
  }
}

const favorite = async (row) => {
  await addFavorite({ universityId: row.universityId, majorId: row.majorId })
  ElMessage.success('已加入收藏')
}

const resize = () => chart?.resize()

onMounted(() => {
  window.addEventListener('resize', resize)
  submit()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})
</script>

<template>
  <div class="recommend-layout">
    <section class="page-card filter-card">
      <div class="page-heading">
        <div>
          <h2>冲稳保辅助筛选</h2>
          <p>匹配分由位次差异和分数差异构成，等级用于整理资料，不代表录取概率。</p>
        </div>
      </div>

      <el-form :model="form" label-position="top" class="recommend-form">
        <el-form-item label="考试省份">
          <el-select v-model="form.province">
            <el-option label="山西" value="山西" />
          </el-select>
        </el-form-item>
        <el-form-item label="科类或选科">
          <el-select v-model="form.subjectTrack">
            <el-option label="物理类" value="物理类" />
            <el-option label="历史类" value="历史类" />
          </el-select>
        </el-form-item>
        <el-form-item label="高考分数">
          <el-input-number v-model="form.score" :min="0" :max="750" controls-position="right" />
        </el-form-item>
        <el-form-item label="全省位次">
          <el-input-number v-model="form.provinceRank" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="意向省份">
          <el-select v-model="form.preferredProvince" clearable placeholder="不限">
            <el-option label="山西" value="山西" />
            <el-option label="江苏" value="江苏" />
            <el-option label="陕西" value="陕西" />
            <el-option label="山东" value="山东" />
            <el-option label="河南" value="河南" />
          </el-select>
        </el-form-item>
        <el-form-item label="院校层次">
          <el-select v-model="form.universityLevel" clearable placeholder="不限">
            <el-option label="本科" value="本科" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业关键词" class="keyword-field">
          <el-input v-model="form.keyword" clearable placeholder="例如：软件工程" />
        </el-form-item>
        <el-button type="primary" class="submit-button" :loading="loading" @click="submit">生成辅助结果</el-button>
      </el-form>
    </section>

    <section class="page-card result-card">
      <div class="result-head">
        <div>
          <span class="result-kicker">MATCH OVERVIEW</span>
          <h2>结果概览</h2>
        </div>
        <div class="result-count"><strong>{{ results.length }}</strong><span>条参考结果</span></div>
      </div>
      <div ref="chartRef" class="chart"></div>
    </section>

    <section class="page-card table-card">
      <div class="page-heading">
        <div>
          <h2>匹配明细</h2>
          <p>按匹配分从高到低排序，点击收藏后可到“我的收藏”集中比较。</p>
        </div>
      </div>
      <el-table v-loading="loading" :data="results" stripe height="520">
        <el-table-column prop="universityName" label="院校" min-width="150" fixed />
        <el-table-column prop="majorName" label="专业" min-width="160" />
        <el-table-column label="地区" width="110">
          <template #default="{ row }">{{ row.province }} · {{ row.city }}</template>
        </el-table-column>
        <el-table-column prop="latestYear" label="最近年份" width="90" align="center" />
        <el-table-column prop="averageMinScore" label="平均最低分" width="110" align="center" />
        <el-table-column prop="averageMinRank" label="平均最低位次" width="120" align="center" />
        <el-table-column label="匹配分" width="100" align="center">
          <template #default="{ row }"><strong class="match-score">{{ row.matchScore }}</strong></template>
        </el-table-column>
        <el-table-column label="等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :class="levelClass(row.matchLevel)" effect="plain">{{ row.matchLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="匹配依据" min-width="300" show-overflow-tooltip />
        <el-table-column prop="source" label="数据来源" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="favorite(row)">收藏</el-button></template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.recommend-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.8fr);
  gap: 20px;
}

.filter-card,
.table-card {
  grid-column: 1 / -1;
}

.recommend-form {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 0 14px;
  align-items: end;
}

.recommend-form :deep(.el-select),
.recommend-form :deep(.el-input-number) {
  width: 100%;
}

.keyword-field {
  grid-column: span 2;
}

.submit-button {
  min-height: 32px;
  margin-bottom: 18px;
}

.result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.result-kicker {
  color: var(--accent);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.result-head h2 {
  margin: 7px 0 0;
  font-size: 20px;
}

.result-count {
  text-align: right;
}

.result-count strong,
.result-count span {
  display: block;
}

.result-count strong {
  color: var(--brand);
  font-size: 32px;
}

.result-count span {
  color: var(--muted);
  font-size: 11px;
}

.chart {
  width: 100%;
  height: 260px;
  margin-top: 10px;
}

.match-score {
  color: var(--brand);
  font-size: 16px;
}

@media (max-width: 1280px) {
  .recommend-form {
    grid-template-columns: repeat(4, minmax(120px, 1fr));
  }
}
</style>
