<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const formRef = ref()
const saving = ref(false)
const form = reactive({
  nickname: auth.user?.nickname || '',
  province: auth.user?.province || '山西',
  examYear: auth.user?.examYear || 2026,
  subjectTrack: auth.user?.subjectTrack || '物理类',
  score: auth.user?.score || 0,
  provinceRank: auth.user?.provinceRank || 1
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  province: [{ required: true, message: '请选择省份', trigger: 'change' }],
  subjectTrack: [{ required: true, message: '请选择科类', trigger: 'change' }]
}

const save = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    await auth.updateProfile(form)
    ElMessage.success('个人档案已更新')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="profile-layout">
    <section class="page-card profile-card">
      <div class="page-heading">
        <div>
          <h2>个人档案</h2>
          <p>这里的省份、科类、分数和位次会作为冲稳保筛选的默认条件。</p>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="账号">
          <el-input :model-value="auth.user?.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="考试省份" prop="province">
          <el-select v-model="form.province">
            <el-option label="山西" value="山西" />
            <el-option label="陕西" value="陕西" />
            <el-option label="河南" value="河南" />
            <el-option label="山东" value="山东" />
          </el-select>
        </el-form-item>
        <el-form-item label="科类或选科" prop="subjectTrack">
          <el-select v-model="form.subjectTrack">
            <el-option label="物理类" value="物理类" />
            <el-option label="历史类" value="历史类" />
          </el-select>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="考试年份">
            <el-input-number v-model="form.examYear" :min="2020" :max="2035" controls-position="right" />
          </el-form-item>
          <el-form-item label="高考分数">
            <el-input-number v-model="form.score" :min="0" :max="750" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="全省位次">
          <el-input-number v-model="form.provinceRank" :min="1" controls-position="right" />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存档案</el-button>
      </el-form>
    </section>

    <aside class="page-card profile-note">
      <span class="note-kicker">填写提示</span>
      <h3>位次比单年分数更适合横向比较</h3>
      <p>不同年份的试题难度和分数线会变化，系统把位次作为主要指标，同时保留分数差异作为辅助信息。</p>
      <el-divider />
      <p>最终填报前，请以省级招生考试机构和院校官网发布的最新招生章程为准。</p>
    </aside>
  </div>
</template>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: minmax(480px, 0.9fr) minmax(280px, 0.6fr);
  gap: 20px;
}

.profile-card :deep(.el-select),
.profile-card :deep(.el-input-number) {
  width: 100%;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.profile-note {
  align-self: start;
  background:
    linear-gradient(150deg, rgba(31, 78, 121, 0.05), transparent 45%),
    #fff;
}

.note-kicker {
  color: var(--accent);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.profile-note h3 {
  margin: 14px 0;
  font-size: 22px;
  line-height: 1.5;
}

.profile-note p {
  color: var(--muted);
  line-height: 1.9;
  font-size: 13px;
}
</style>
