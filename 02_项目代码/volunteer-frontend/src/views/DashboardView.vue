<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const profile = computed(() => auth.user || {})
</script>

<template>
  <div class="dashboard">
    <section class="welcome-card">
      <div class="welcome-copy">
        <span class="welcome-kicker">今日志愿工作台</span>
        <h2>{{ auth.displayName }}，先从你的成绩位次开始整理</h2>
        <p>系统用最近三年录取记录生成可解释的冲稳保辅助结果，院校详情、收藏和对比信息会保留在同一账号下。</p>
        <div class="welcome-actions">
          <el-button type="primary" size="large" @click="router.push('/recommendations')">
            生成辅助结果
          </el-button>
          <el-button size="large" @click="router.push('/universities')">查询院校</el-button>
        </div>
      </div>
      <div class="score-card">
        <span>当前档案</span>
        <strong>{{ profile.score || '--' }}<small>分</small></strong>
        <div class="score-meta">
          <div><span>省份</span><b>{{ profile.province || '--' }}</b></div>
          <div><span>科类</span><b>{{ profile.subjectTrack || '--' }}</b></div>
          <div><span>位次</span><b>{{ profile.provinceRank || '--' }}</b></div>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <article>
        <span>参考年份</span>
        <strong>近 3 年</strong>
        <p>按考试年份向前取三年录取记录</p>
      </article>
      <article>
        <span>位次权重</span>
        <strong>70%</strong>
        <p>位次比单年分数更稳定</p>
      </article>
      <article>
        <span>结果上限</span>
        <strong>50 条</strong>
        <p>控制页面信息量并便于比较</p>
      </article>
      <article>
        <span>数据口径</span>
        <strong>可追溯</strong>
        <p>每条结果展示数据来源</p>
      </article>
    </section>

    <section class="workflow">
      <div class="page-heading">
        <div>
          <h2>建议使用顺序</h2>
          <p>先核对个人条件，再查院校，最后整理冲稳保和收藏。</p>
        </div>
      </div>
      <div class="workflow-grid">
        <button @click="router.push('/profile')">
          <span>01</span>
          <strong>核对个人档案</strong>
          <small>确认省份、科类、分数和位次</small>
        </button>
        <button @click="router.push('/universities')">
          <span>02</span>
          <strong>检索院校专业</strong>
          <small>按地区、层次和关键词筛选</small>
        </button>
        <button @click="router.push('/recommendations')">
          <span>03</span>
          <strong>整理冲稳保</strong>
          <small>查看匹配分、依据和来源</small>
        </button>
        <button @click="router.push('/favorites')">
          <span>04</span>
          <strong>收藏并复核</strong>
          <small>用官方招生章程完成最终核验</small>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 24px;
}

.welcome-card {
  min-height: 300px;
  padding: 42px;
  border-radius: 20px;
  color: #fff;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(300px, 0.6fr);
  gap: 40px;
  background:
    radial-gradient(circle at 90% 15%, rgba(231, 191, 116, 0.22), transparent 26%),
    linear-gradient(125deg, #17334a, #245d83);
  box-shadow: 0 20px 50px rgba(20, 43, 64, 0.18);
}

.welcome-copy {
  align-self: center;
}

.welcome-kicker {
  color: #e7bf74;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.welcome-copy h2 {
  max-width: 660px;
  margin: 15px 0;
  font-size: 34px;
  line-height: 1.3;
  letter-spacing: -0.04em;
}

.welcome-copy p {
  max-width: 670px;
  margin: 0;
  color: #c3d2df;
  line-height: 1.8;
  font-size: 13px;
}

.welcome-actions {
  margin-top: 28px;
  display: flex;
  gap: 12px;
}

.score-card {
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
  align-self: center;
}

.score-card > span {
  color: #9fb4c6;
  font-size: 12px;
}

.score-card > strong {
  display: block;
  margin: 12px 0 24px;
  font-size: 54px;
  letter-spacing: -0.06em;
}

.score-card small {
  margin-left: 5px;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0;
}

.score-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.score-meta div {
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}

.score-meta span,
.score-meta b {
  display: block;
}

.score-meta span {
  color: #9fb4c6;
  font-size: 10px;
}

.score-meta b {
  margin-top: 5px;
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.metric-grid article {
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
}

.metric-grid span {
  color: var(--muted);
  font-size: 11px;
}

.metric-grid strong {
  display: block;
  margin: 10px 0 8px;
  color: var(--brand);
  font-size: 24px;
}

.metric-grid p {
  margin: 0;
  color: #8a95a6;
  font-size: 11px;
}

.workflow {
  padding: 26px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fff;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.workflow-grid button {
  min-height: 138px;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 12px;
  text-align: left;
  color: var(--ink);
  background: #f9fbfd;
  cursor: pointer;
  transition: 0.2s ease;
}

.workflow-grid button:hover {
  transform: translateY(-3px);
  border-color: #aac2d7;
  box-shadow: 0 12px 28px rgba(31, 78, 121, 0.1);
}

.workflow-grid span,
.workflow-grid strong,
.workflow-grid small {
  display: block;
}

.workflow-grid span {
  color: var(--accent);
  font-size: 11px;
}

.workflow-grid strong {
  margin: 16px 0 8px;
  font-size: 15px;
}

.workflow-grid small {
  color: var(--muted);
  line-height: 1.6;
}
</style>
