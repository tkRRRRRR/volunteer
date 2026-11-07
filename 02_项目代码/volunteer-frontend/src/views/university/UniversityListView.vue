<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUniversities } from '../../api/university'
import { addFavorite } from '../../api/favorite'

const loading = ref(false)
const universities = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const selected = ref(null)
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

const openDetail = (row) => {
  selected.value = row
  drawerVisible.value = true
}

const favorite = async (row) => {
  await addFavorite({ universityId: row.id, majorId: null })
  ElMessage.success('已加入收藏')
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div>
        <h2>院校查询</h2>
        <p>演示数据中的院校名称均为虚构，页面结构用于展示实际业务的查询与详情功能。</p>
      </div>
      <el-tag type="warning" effect="plain">演示数据</el-tag>
    </div>

    <div class="filter-row">
      <el-input v-model="query.keyword" clearable placeholder="院校名称、标签或专业方向" style="width: 260px" @keyup.enter="load" />
      <el-select v-model="query.province" clearable placeholder="所在省份" style="width: 150px">
        <el-option label="山西" value="山西" />
        <el-option label="江苏" value="江苏" />
        <el-option label="陕西" value="陕西" />
        <el-option label="山东" value="山东" />
        <el-option label="河南" value="河南" />
      </el-select>
      <el-select v-model="query.level" clearable placeholder="院校层次" style="width: 150px">
        <el-option label="本科" value="本科" />
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

    <el-drawer v-model="drawerVisible" title="院校详情" size="460px">
      <template v-if="selected">
        <div class="detail-title">
          <span>院校档案</span>
          <h3>{{ selected.name }}</h3>
          <p>{{ selected.province }} · {{ selected.city }} · {{ selected.level }} · {{ selected.type }}</p>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="院校标签">{{ selected.tags }}</el-descriptions-item>
          <el-descriptions-item label="院校简介">{{ selected.description }}</el-descriptions-item>
          <el-descriptions-item label="官网">{{ selected.website }}</el-descriptions-item>
        </el-descriptions>
        <el-alert
          class="detail-alert"
          title="该页面为演示院校，不能作为真实招生信息使用。"
          type="warning"
          :closable="false"
          show-icon
        />
      </template>
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
