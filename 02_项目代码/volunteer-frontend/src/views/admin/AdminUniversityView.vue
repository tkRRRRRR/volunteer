<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminCatalogDrawer from '../../components/admin/AdminCatalogDrawer.vue'
import {
  createUniversity,
  deleteUniversity,
  getUniversities,
  updateUniversity
} from '../../api/university'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const catalogVisible = ref(false)
const catalogUniversity = ref(null)
const editingId = ref(null)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })
const formRef = ref()
const form = reactive({
  name: '',
  province: '山西',
  city: '',
  level: '本科',
  type: '综合',
  description: '',
  website: '',
  tags: ''
})

const rules = {
  name: [{ required: true, message: '请输入院校名称', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  level: [{ required: true, message: '请输入层次', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const page = await getUniversities(query)
    rows.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingId.value = null
  Object.assign(form, {
    name: '',
    province: '山西',
    city: '',
    level: '本科',
    type: '综合',
    description: '',
    website: '',
    tags: ''
  })
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const openCatalog = (row) => {
  catalogUniversity.value = row
  catalogVisible.value = true
}

const save = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateUniversity(editingId.value, form)
      ElMessage.success('院校已更新')
    } else {
      await createUniversity(form)
      ElMessage.success('院校已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除“${row.name}”吗？`, '删除院校', { type: 'warning' })
  await deleteUniversity(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div>
        <h2>院校数据维护</h2>
        <p>维护院校基础信息，并进入数据维护抽屉管理专业和历年录取记录。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增院校</el-button>
    </div>

    <div class="filter-row">
      <el-input v-model="query.keyword" clearable placeholder="院校名称" style="width: 260px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="院校" min-width="190" />
      <el-table-column label="地区" width="150">
        <template #default="{ row }">{{ row.province }} · {{ row.city }}</template>
      </el-table-column>
      <el-table-column prop="level" label="层次" width="90" />
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="tags" label="标签" min-width="180" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="openCatalog(row)">数据维护</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.page"
        layout="total, prev, pager, next"
        :page-size="query.size"
        :total="total"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑院校' : '新增院校'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="admin-form">
        <el-form-item label="院校名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" />
        </el-form-item>
        <el-form-item label="层次" prop="level">
          <el-input v-model="form.level" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="form.type" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" />
        </el-form-item>
        <el-form-item label="官网" class="span-2">
          <el-input v-model="form.website" />
        </el-form-item>
        <el-form-item label="简介" class="span-2">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <AdminCatalogDrawer
      v-model:visible="catalogVisible"
      :university="catalogUniversity"
    />
  </div>
</template>

<style scoped>
.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}

.admin-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.span-2 {
  grid-column: 1 / -1;
}
</style>
