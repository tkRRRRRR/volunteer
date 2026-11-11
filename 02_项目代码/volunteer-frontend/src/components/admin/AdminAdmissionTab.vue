<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdmissionRecord,
  deleteAdmissionRecord,
  getAdmissionRecords,
  listMajors,
  updateAdmissionRecord
} from '../../api/adminCatalog'
import {
  buildAdmissionPayload,
  buildAdmissionQuery,
  canCreateAdmissionRecord,
  createEmptyAdmissionForm
} from '../../utils/adminCatalog'

const props = defineProps({
  universityId: {
    type: Number,
    required: true
  },
  majorRevision: {
    type: Number,
    default: 0
  }
})

const currentYear = new Date().getFullYear()
const loadingMajors = ref(false)
const loading = ref(false)
const saving = ref(false)
const majors = ref([])
const rows = ref([])
const total = ref(0)
const errorMessage = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const filters = reactive({
  page: 1,
  size: 10,
  majorId: '',
  year: null,
  province: '',
  subjectTrack: ''
})
const form = reactive(createEmptyAdmissionForm(currentYear))

const canCreate = computed(() => canCreateAdmissionRecord(majors.value))
const rules = {
  majorId: [{ required: true, message: '请选择具体专业', trigger: 'change' }],
  province: [{ required: true, message: '请输入招生省份', trigger: 'blur' }],
  year: [{ required: true, message: '请输入录取年份', trigger: 'change' }],
  subjectTrack: [{ required: true, message: '请输入科类或选科组合', trigger: 'blur' }],
  batch: [{ required: true, message: '请输入录取批次', trigger: 'blur' }],
  minScore: [{ required: true, message: '请输入最低分', trigger: 'change' }],
  minRank: [{ required: true, message: '请输入最低位次', trigger: 'change' }],
  source: [{ required: true, message: '请输入数据来源', trigger: 'blur' }]
}

const loadMajors = async () => {
  loadingMajors.value = true
  try {
    majors.value = await listMajors(props.universityId)
    if (filters.majorId && !majors.value.some((major) => major.id === filters.majorId)) {
      filters.majorId = ''
    }
  } catch (error) {
    majors.value = []
    errorMessage.value = error.message || '专业选项加载失败'
  } finally {
    loadingMajors.value = false
  }
}

const loadRecords = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const page = await getAdmissionRecords(
      props.universityId,
      buildAdmissionQuery(filters)
    )
    rows.value = page.records
    total.value = page.total
  } catch (error) {
    rows.value = []
    total.value = 0
    errorMessage.value = error.message || '录取记录加载失败'
  } finally {
    loading.value = false
  }
}

const reload = async () => {
  await loadMajors()
  await loadRecords()
}

const search = () => {
  filters.page = 1
  loadRecords()
}

const resetFilters = () => {
  Object.assign(filters, {
    page: 1,
    majorId: '',
    year: null,
    province: '',
    subjectTrack: ''
  })
  loadRecords()
}

const resetForm = () => {
  editingId.value = null
  Object.assign(form, createEmptyAdmissionForm(currentYear))
}

const openCreate = () => {
  if (!canCreate.value) {
    ElMessage.warning('请先在专业维护页签中为该院校添加专业')
    return
  }
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    majorId: row.majorId,
    province: row.province || '',
    year: row.year,
    subjectTrack: row.subjectTrack || '',
    batch: row.batch || '',
    minScore: row.minScore,
    minRank: row.minRank,
    planCount: row.planCount,
    source: row.source || '演示数据（非官方）'
  })
  dialogVisible.value = true
}

const save = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = buildAdmissionPayload(form)
    if (editingId.value) {
      await updateAdmissionRecord(props.universityId, editingId.value, payload)
      ElMessage.success('录取记录已更新')
    } else {
      await createAdmissionRecord(props.universityId, payload)
      ElMessage.success('录取记录已创建')
    }
    dialogVisible.value = false
    await loadRecords()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${row.year} 年“${row.majorName}”的录取记录吗？`,
      '删除录取记录',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteAdmissionRecord(props.universityId, row.id)
  ElMessage.success('录取记录已删除')
  await loadRecords()
}

watch(
  [() => props.universityId, () => props.majorRevision],
  async () => {
    filters.page = 1
    await reload()
  }
)
onMounted(reload)
</script>

<template>
  <div class="tab-toolbar">
    <div class="filter-grid">
      <el-select
        v-model="filters.majorId"
        clearable
        :loading="loadingMajors"
        placeholder="全部专业"
        style="width: 170px"
      >
        <el-option
          v-for="major in majors"
          :key="major.id"
          :label="major.name"
          :value="major.id"
        />
      </el-select>
      <el-input-number
        v-model="filters.year"
        :min="2000"
        :max="currentYear"
        :controls="false"
        placeholder="年份"
        style="width: 110px"
      />
      <el-input v-model="filters.province" clearable placeholder="招生省份" style="width: 130px" />
      <el-input v-model="filters.subjectTrack" clearable placeholder="科类" style="width: 130px" />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>
    <el-tooltip
      :disabled="canCreate"
      content="请先添加专业"
      placement="top"
    >
      <span>
        <el-button type="primary" :disabled="!canCreate" @click="openCreate">
          新增录取记录
        </el-button>
      </span>
    </el-tooltip>
  </div>

  <el-alert
    v-if="errorMessage"
    :title="errorMessage"
    type="error"
    :closable="false"
    show-icon
  >
    <template #default>
      <el-button link type="primary" @click="reload">重新加载</el-button>
    </template>
  </el-alert>

  <el-table
    v-loading="loading"
    :data="rows"
    stripe
    empty-text="当前筛选条件下暂无录取记录"
  >
    <el-table-column prop="year" label="年份" width="80" />
    <el-table-column prop="majorName" label="专业" min-width="160" />
    <el-table-column prop="province" label="省份" width="90" />
    <el-table-column prop="subjectTrack" label="科类" width="100" />
    <el-table-column prop="batch" label="批次" width="100" />
    <el-table-column prop="minScore" label="最低分" width="85" />
    <el-table-column prop="minRank" label="最低位次" width="100" />
    <el-table-column prop="planCount" label="计划数" width="85" />
    <el-table-column prop="source" label="来源" min-width="145" show-overflow-tooltip />
    <el-table-column label="操作" width="130" fixed="right">
      <template #default="{ row }">
        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        <el-button link type="danger" @click="remove(row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>

  <div class="pagination-row">
    <el-pagination
      v-model:current-page="filters.page"
      layout="total, prev, pager, next"
      :page-size="filters.size"
      :total="total"
      @current-change="loadRecords"
    />
  </div>

  <el-dialog
    v-model="dialogVisible"
    :title="editingId ? '编辑录取记录' : '新增录取记录'"
    width="680px"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="专业" prop="majorId">
        <el-select v-model="form.majorId" placeholder="请选择具体专业" style="width: 100%">
          <el-option
            v-for="major in majors"
            :key="major.id"
            :label="major.name"
            :value="major.id"
          />
        </el-select>
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="招生省份" prop="province">
          <el-input v-model="form.province" maxlength="30" />
        </el-form-item>
        <el-form-item label="录取年份" prop="year">
          <el-input-number
            v-model="form.year"
            :min="2000"
            :max="currentYear"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="科类或选科组合" prop="subjectTrack">
          <el-input v-model="form.subjectTrack" maxlength="30" />
        </el-form-item>
        <el-form-item label="录取批次" prop="batch">
          <el-input v-model="form.batch" maxlength="30" />
        </el-form-item>
        <el-form-item label="最低分" prop="minScore">
          <el-input-number v-model="form.minScore" :min="0" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低位次" prop="minRank">
          <el-input-number v-model="form.minRank" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划数">
          <el-input-number v-model="form.planCount" :min="0" style="width: 100%" />
        </el-form-item>
      </div>
      <el-form-item label="数据来源" prop="source">
        <el-input v-model="form.source" maxlength="255" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tab-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.filter-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
}

:deep(.el-alert) {
  margin-bottom: 14px;
}
</style>
