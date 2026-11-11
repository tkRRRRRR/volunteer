<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  downloadAdmissionImportTemplate,
  importAdmissionRecords
} from '../../api/adminCatalog'
import {
  formatFileSize,
  validateImportFile
} from '../../utils/adminCatalog'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  universityId: {
    type: Number,
    required: true
  },
  universityName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:visible', 'imported'])
const uploadRef = ref()
const fileList = ref([])
const selectedFile = ref(null)
const importing = ref(false)
const downloading = ref(false)
const result = ref(null)
const errorMessage = ref('')

const fileSummary = computed(() => selectedFile.value
  ? `${selectedFile.value.name} · ${formatFileSize(selectedFile.value.size)}`
  : '')

const handleChange = (uploadFile, uploadFiles) => {
  const raw = uploadFile.raw
  const validation = validateImportFile(raw)
  if (!validation.valid) {
    ElMessage.error(validation.message)
    selectedFile.value = null
    fileList.value = []
    uploadRef.value?.clearFiles()
    return
  }
  selectedFile.value = raw
  fileList.value = uploadFiles.slice(-1)
  result.value = null
  errorMessage.value = ''
}

const handleRemove = () => {
  selectedFile.value = null
  result.value = null
  errorMessage.value = ''
}

const handleExceed = () => {
  ElMessage.warning('一次只能选择一个文件，请先移除当前文件')
}

const downloadTemplate = async () => {
  downloading.value = true
  try {
    await downloadAdmissionImportTemplate(props.universityId)
    ElMessage.success('Excel 模板已下载')
  } catch (error) {
    ElMessage.error(error.message || 'Excel 模板下载失败')
  } finally {
    downloading.value = false
  }
}

const submitImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 CSV 或 Excel 文件')
    return
  }
  importing.value = true
  errorMessage.value = ''
  try {
    result.value = await importAdmissionRecords(
      props.universityId,
      selectedFile.value
    )
    if (result.value.successCount > 0) {
      ElMessage.success(`成功导入 ${result.value.successCount} 条记录`)
      emit('imported')
    } else {
      ElMessage.warning('没有可导入的合法记录')
    }
  } catch (error) {
    errorMessage.value = error.message || '文件导入失败'
  } finally {
    importing.value = false
  }
}

const reset = () => {
  selectedFile.value = null
  fileList.value = []
  result.value = null
  errorMessage.value = ''
  uploadRef.value?.clearFiles()
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      reset()
    }
  }
)
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="批量导入录取记录"
    width="760px"
    @update:model-value="emit('update:visible', $event)"
    @closed="reset"
  >
    <div class="import-intro">
      <div>
        <strong>{{ universityName }}</strong>
        <p>支持 CSV、XLSX 和 XLS，文件不超过 2 MB，最多 1000 条数据。</p>
      </div>
      <el-button :loading="downloading" @click="downloadTemplate">
        下载 Excel 模板
      </el-button>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      class="import-upload"
      drag
      :auto-upload="false"
      :limit="1"
      accept=".csv,.xlsx,.xls"
      :on-change="handleChange"
      :on-remove="handleRemove"
      :on-exceed="handleExceed"
    >
      <div class="upload-mark">CSV / XLSX / XLS</div>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击选择</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          专业编号和专业名称必须匹配当前院校已有专业。
        </div>
      </template>
    </el-upload>

    <div v-if="fileSummary" class="file-summary">{{ fileSummary }}</div>

    <div v-if="result" class="result-panel">
      <div class="summary-grid">
        <div><span>总行数</span><strong>{{ result.totalRows }}</strong></div>
        <div class="success"><span>成功</span><strong>{{ result.successCount }}</strong></div>
        <div class="failed"><span>失败</span><strong>{{ result.failedCount }}</strong></div>
      </div>

      <el-table
        v-if="result.errors?.length"
        :data="result.errors"
        max-height="300"
        stripe
      >
        <el-table-column prop="rowNumber" label="行号" width="75" />
        <el-table-column prop="majorId" label="专业编号" width="100" />
        <el-table-column prop="majorName" label="专业名称" min-width="140" />
        <el-table-column prop="message" label="错误原因" min-width="280" />
      </el-table>
      <el-empty
        v-else
        :image-size="70"
        description="文件中的记录已全部导入"
      />
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">关闭</el-button>
      <el-button
        type="primary"
        :loading="importing"
        :disabled="!selectedFile"
        @click="submitImport"
      >
        开始导入
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.import-intro {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid #dce7f5;
  border-radius: 12px;
  background: #f5f9ff;
}

.import-intro p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.import-upload {
  margin-bottom: 12px;
}

.upload-mark {
  margin-bottom: 8px;
  color: #1f4e79;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.file-summary {
  margin: 10px 0 16px;
  color: #52627a;
  font-size: 13px;
}

.result-panel {
  margin-top: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.summary-grid > div {
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
}

.summary-grid span,
.summary-grid strong {
  display: block;
}

.summary-grid span {
  color: #718096;
  font-size: 12px;
}

.summary-grid strong {
  margin-top: 5px;
  font-size: 22px;
}

.summary-grid .success strong {
  color: #1f8f5f;
}

.summary-grid .failed strong {
  color: #c2413b;
}

:deep(.el-alert) {
  margin-bottom: 14px;
}
</style>
