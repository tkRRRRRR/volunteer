<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mode = ref('login')
const submitting = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({
  username: 'student',
  password: 'student123'
})

const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  province: '山西',
  examYear: 2026,
  subjectTrack: '物理类',
  score: 560,
  provinceRank: 10000
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  province: [{ required: true, message: '请选择省份', trigger: 'change' }],
  subjectTrack: [{ required: true, message: '请选择科类', trigger: 'change' }],
  score: [{ required: true, message: '请输入分数', trigger: 'blur' }],
  provinceRank: [{ required: true, message: '请输入位次', trigger: 'blur' }]
}

const submitLogin = async () => {
  await loginFormRef.value?.validate()
  submitting.value = true
  try {
    await auth.login(loginForm)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/')
  } finally {
    submitting.value = false
  }
}

const submitRegister = async () => {
  await registerFormRef.value?.validate()
  submitting.value = true
  try {
    await auth.register(registerForm)
    ElMessage.success('注册成功')
    router.replace('/')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-intro">
      <div class="intro-badge">本科毕业设计系统</div>
      <h1>把分数、位次和历年录取数据放在同一张视图里</h1>
      <p>
        系统用位次为主、分数为辅的可解释规则，整理冲刺、稳妥、保底院校，
        帮助你完成资料查询、志愿比较与收藏管理。
      </p>
      <div class="intro-points">
        <div><span>01</span><strong>历史数据可追溯</strong><small>每条结果保留年份与来源</small></div>
        <div><span>02</span><strong>匹配过程可解释</strong><small>展示平均最低分与位次差异</small></div>
        <div><span>03</span><strong>辅助结果不承诺</strong><small>最终填报仍需核对官方政策</small></div>
      </div>
      <div class="demo-note">
        演示学生：student / student123<br />
        演示管理员：admin / admin123
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-panel-header">
        <div class="mini-logo">志</div>
        <div>
          <strong>志愿导航</strong>
          <span>高考志愿填报辅助系统</span>
        </div>
      </div>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="账号登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-position="top" @keyup.enter="submitLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" size="large" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" show-password size="large" placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" size="large" class="auth-submit" :loading="submitting" @click="submitLogin">
              进入系统
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="学生注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="rules"
            label-position="top"
            class="register-grid"
            @keyup.enter="submitRegister"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="3到30个字符" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="至少6位" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="考试省份" prop="province">
              <el-select v-model="registerForm.province">
                <el-option label="山西" value="山西" />
                <el-option label="陕西" value="陕西" />
                <el-option label="河南" value="河南" />
                <el-option label="山东" value="山东" />
              </el-select>
            </el-form-item>
            <el-form-item label="科类或选科" prop="subjectTrack">
              <el-select v-model="registerForm.subjectTrack">
                <el-option label="物理类" value="物理类" />
                <el-option label="历史类" value="历史类" />
              </el-select>
            </el-form-item>
            <el-form-item label="高考分数" prop="score">
              <el-input-number v-model="registerForm.score" :min="0" :max="750" controls-position="right" />
            </el-form-item>
            <el-form-item label="全省位次" prop="provinceRank">
              <el-input-number v-model="registerForm.provinceRank" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="考试年份">
              <el-input-number v-model="registerForm.examYear" :min="2020" :max="2035" controls-position="right" />
            </el-form-item>
            <el-button type="primary" size="large" class="auth-submit" :loading="submitting" @click="submitRegister">
              创建学生账号
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(440px, 0.8fr);
  background:
    linear-gradient(120deg, rgba(19, 43, 64, 0.96), rgba(27, 72, 103, 0.9)),
    #142b40;
}

.auth-intro {
  padding: 9vh 8vw;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    radial-gradient(circle at 10% 90%, rgba(231, 191, 116, 0.16), transparent 25%),
    radial-gradient(circle at 90% 10%, rgba(122, 176, 213, 0.15), transparent 30%);
}

.intro-badge {
  width: fit-content;
  padding: 7px 12px;
  border: 1px solid rgba(231, 191, 116, 0.4);
  border-radius: 999px;
  color: #f0d59f;
  font-size: 11px;
  letter-spacing: 0.12em;
}

.auth-intro h1 {
  max-width: 680px;
  margin: 28px 0 20px;
  font-size: clamp(38px, 4vw, 64px);
  line-height: 1.16;
  letter-spacing: -0.055em;
}

.auth-intro > p {
  max-width: 620px;
  margin: 0;
  color: #c3d0dc;
  font-size: 15px;
  line-height: 2;
}

.intro-points {
  max-width: 660px;
  margin-top: 52px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.intro-points div {
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.16);
}

.intro-points span,
.intro-points strong,
.intro-points small {
  display: block;
}

.intro-points span {
  color: #e7bf74;
  font-size: 11px;
}

.intro-points strong {
  margin-top: 9px;
  font-size: 14px;
}

.intro-points small {
  margin-top: 7px;
  color: #9eb0c1;
  line-height: 1.5;
}

.demo-note {
  margin-top: 48px;
  color: #8fa5b8;
  font-size: 12px;
  line-height: 1.8;
}

.auth-panel {
  min-height: 100vh;
  padding: 64px clamp(34px, 5vw, 76px);
  background: #f7f9fc;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-panel-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 28px;
}

.mini-logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  background: #1f4e79;
  font-size: 20px;
  font-weight: 800;
}

.auth-panel-header strong,
.auth-panel-header span {
  display: block;
}

.auth-panel-header strong {
  font-size: 16px;
}

.auth-panel-header span {
  margin-top: 4px;
  color: #778396;
  font-size: 11px;
}

.auth-submit {
  width: 100%;
  margin-top: 6px;
}

.register-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 14px;
}

.register-grid :deep(.el-select),
.register-grid :deep(.el-input-number) {
  width: 100%;
}

.register-grid .auth-submit {
  grid-column: 1 / -1;
}

.auth-panel :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

.auth-panel :deep(.el-form-item__label) {
  color: #4b586b;
  font-size: 12px;
}
</style>
