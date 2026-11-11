<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFavorites, removeFavorite } from '../../api/favorite'
import { getUniversityOverview } from '../../api/university'
import { useAuthStore } from '../../stores/auth'
import { buildComparisonRows, canCompareFavorites } from '../../utils/favoriteComparison'

const auth = useAuthStore()
const loading = ref(false)
const favorites = ref([])
const tableRef = ref()
const selectedFavorites = ref([])
const compareVisible = ref(false)
const compareLoading = ref(false)
const comparisonRows = ref([])

const compareEnabled = computed(() => canCompareFavorites(selectedFavorites.value.length))

const load = async () => {
  loading.value = true
  try {
    favorites.value = await getFavorites()
  } finally {
    loading.value = false
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定取消收藏“${row.universityName}”吗？`, '取消收藏', {
    type: 'warning'
  })
  await removeFavorite(row.id)
  ElMessage.success('已取消收藏')
  load()
}

const handleSelectionChange = (rows) => {
  if (rows.length > 4) {
    ElMessage.warning('最多选择 4 项进行对比')
    tableRef.value?.clearSelection()
    selectedFavorites.value = []
    return
  }
  selectedFavorites.value = rows
}

const compare = async () => {
  if (!compareEnabled.value) {
    ElMessage.warning('请选择 2 至 4 项收藏进行对比')
    return
  }

  compareVisible.value = true
  compareLoading.value = true
  const detailByUniversityId = new Map()
  const universityIds = [...new Set(selectedFavorites.value.map((item) => item.universityId))]
  try {
    await Promise.all(universityIds.map(async (universityId) => {
      try {
        const detail = await getUniversityOverview(universityId, {
          province: auth.user?.province || undefined,
          subjectTrack: auth.user?.subjectTrack || undefined
        })
        detailByUniversityId.set(universityId, detail)
      } catch {
        // 单个详情失败时保留其他对比项，由数据转换工具标记失败行。
      }
    }))
    comparisonRows.value = buildComparisonRows(selectedFavorites.value, detailByUniversityId)
  } finally {
    compareLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div>
        <h2>我的收藏</h2>
        <p>勾选 2 至 4 项后可横向比较院校专业和最近录取数据。</p>
      </div>
      <div class="favorite-actions">
        <el-tag effect="plain">{{ favorites.length }} 条</el-tag>
        <el-button type="primary" :disabled="!compareEnabled" @click="compare">
          对比选中（{{ selectedFavorites.length }}/4）
        </el-button>
      </div>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="favorites"
      row-key="id"
      stripe
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="universityName" label="院校" min-width="190" />
      <el-table-column prop="majorName" label="专业" min-width="180" />
      <el-table-column label="地区" width="150">
        <template #default="{ row }">{{ row.province }} · {{ row.city }}</template>
      </el-table-column>
      <el-table-column prop="universityLevel" label="层次" width="100" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }"><el-button link type="danger" @click="remove(row)">取消收藏</el-button></template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && !favorites.length" class="empty-panel">
      还没有收藏记录，可以先去院校查询或冲稳保筛选中添加。
    </div>

    <el-dialog v-model="compareVisible" title="收藏横向对比" width="1080px">
      <el-table v-loading="compareLoading" :data="comparisonRows" border>
        <el-table-column prop="universityName" label="院校" min-width="160" />
        <el-table-column prop="majorName" label="专业" min-width="150" />
        <el-table-column label="地区" width="120">
          <template #default="{ row }">{{ row.province }} · {{ row.city }}</template>
        </el-table-column>
        <el-table-column prop="universityLevel" label="层次" width="80" />
        <el-table-column prop="latestYear" label="最近年份" width="90" align="center" />
        <el-table-column prop="minScore" label="最低分" width="90" align="center" />
        <el-table-column prop="minRank" label="最低位次" width="100" align="center" />
        <el-table-column label="数据来源" min-width="160">
          <template #default="{ row }">
            <span v-if="row.loadError" class="load-error">详情加载失败</span>
            <span v-else>{{ row.source || '暂无数据' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <p class="compare-note">对比数据仅来自演示录取记录，不代表真实录取结果。</p>
    </el-dialog>
  </div>
</template>

<style scoped>
.favorite-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.load-error {
  color: #c45656;
}

.compare-note {
  margin: 14px 0 0;
  color: var(--muted);
  font-size: 12px;
}
</style>
