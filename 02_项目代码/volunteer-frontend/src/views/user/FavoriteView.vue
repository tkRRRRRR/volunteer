<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFavorites, removeFavorite } from '../../api/favorite'

const loading = ref(false)
const favorites = ref([])

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

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div>
        <h2>我的收藏</h2>
        <p>把候选院校专业集中到这里，后续再按招生章程、城市和培养方案逐项比较。</p>
      </div>
      <el-tag effect="plain">{{ favorites.length }} 条</el-tag>
    </div>

    <el-table v-loading="loading" :data="favorites" stripe>
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
  </div>
</template>
