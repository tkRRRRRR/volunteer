<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createMajor,
  deleteMajor,
  listMajors,
  updateMajor
} from '../../api/adminCatalog'
import { buildMajorPayload } from '../../utils/adminCatalog'

const props = defineProps({
  universityId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['changed'])
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const errorMessage = ref('')
const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  name: '',
  category: '',
  subjectRequirement: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入专业名称', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    rows.value = await listMajors(props.universityId, {
      keyword: keyword.value.trim() || undefined
    })
  } catch (error) {
    rows.value = []
    errorMessage.value = error.message || '专业数据加载失败'
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingId.value = null
  Object.assign(form, {
    name: '',
    category: '',
    subjectRequirement: '',
    description: ''
  })
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name || '',
    category: row.category || '',
    subjectRequirement: row.subjectRequirement || '',
    description: row.description || ''
  })
  dialogVisible.value = true
}

const save = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = buildMajorPayload(form)
    if (editingId.value) {
      await updateMajor(props.universityId, editingId.value, payload)
      ElMessage.success('专业已更新')
    } else {
      await createMajor(props.universityId, payload)
      ElMessage.success('专业已创建')
    }
    dialogVisible.value = false
    await load()
    emit('changed')
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除“${row.name}”吗？`, '删除专业', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteMajor(props.universityId, row.id)
  ElMessage.success('专业已删除')
  await load()
  emit('changed')
}

watch(() => props.universityId, load)
onMounted(load)
</script>

<template>
  <div class="tab-toolbar">
    <div class="filter-row">
      <el-input
        v-model="keyword"
        clearable
        placeholder="专业名称或门类"
        style="width: 260px"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-button type="primary" @click="openCreate">新增专业</el-button>
  </div>

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

  <el-table
    v-loading="loading"
    :data="rows"
    stripe
    empty-text="当前院校暂无专业，请先新增专业"
  >
    <el-table-column prop="name" label="专业名称" min-width="180" />
    <el-table-column prop="category" label="门类" width="100" />
    <el-table-column prop="subjectRequirement" label="选科要求" min-width="150" />
    <el-table-column prop="description" label="简介" min-width="240" show-overflow-tooltip />
    <el-table-column label="操作" width="130" fixed="right">
      <template #default="{ row }">
        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        <el-button link type="danger" @click="remove(row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>

  <el-dialog
    v-model="dialogVisible"
    :title="editingId ? '编辑专业' : '新增专业'"
    width="580px"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="专业名称" prop="name">
        <el-input v-model="form.name" maxlength="100" show-word-limit />
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="专业门类">
          <el-input v-model="form.category" maxlength="50" />
        </el-form-item>
        <el-form-item label="选科要求">
          <el-input v-model="form.subjectRequirement" maxlength="100" />
        </el-form-item>
      </div>
      <el-form-item label="专业简介">
        <el-input v-model="form.description" type="textarea" :rows="4" />
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
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  gap: 10px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

:deep(.el-alert) {
  margin-bottom: 14px;
}
</style>
