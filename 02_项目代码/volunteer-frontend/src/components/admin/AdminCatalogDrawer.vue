<script setup>
import { ref, watch } from 'vue'
import AdminAdmissionTab from './AdminAdmissionTab.vue'
import AdminMajorTab from './AdminMajorTab.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  university: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible'])
const activeTab = ref('majors')
const majorRevision = ref(0)

watch(
  () => props.university?.id,
  () => {
    activeTab.value = 'majors'
    majorRevision.value += 1
  }
)

const handleMajorChanged = () => {
  // 专业增删改后通知录取页签重新加载专业选择器。
  majorRevision.value += 1
}
</script>

<template>
  <el-drawer
    :model-value="visible"
    size="min(1080px, 92vw)"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <template #header>
      <div v-if="university" class="drawer-heading">
        <div>
          <span class="section-kicker">院校数据维护</span>
          <h2>{{ university.name }}</h2>
        </div>
        <el-tag effect="plain">{{ university.province }} · {{ university.city }}</el-tag>
      </div>
    </template>

    <div v-if="university" class="catalog-body">
      <div class="context-note">
        专业和录取记录只在本院校范围内维护。录取记录必须选择当前院校下的具体专业。
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="专业维护" name="majors">
          <AdminMajorTab
            :university-id="university.id"
            @changed="handleMajorChanged"
          />
        </el-tab-pane>
        <el-tab-pane label="录取记录" name="admissions" lazy>
          <AdminAdmissionTab
            :university-id="university.id"
            :university-name="university.name"
            :major-revision="majorRevision"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-drawer>
</template>

<style scoped>
.drawer-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: 100%;
}

.drawer-heading h2 {
  margin: 6px 0 0;
  font-size: 22px;
}

.section-kicker {
  color: #728096;
  font-size: 12px;
  letter-spacing: 0.12em;
}

.catalog-body {
  min-height: 60vh;
}

.context-note {
  margin-bottom: 18px;
  padding: 12px 16px;
  border: 1px solid #dce7f5;
  border-radius: 12px;
  background: #f5f9ff;
  color: #52627a;
  line-height: 1.7;
}
</style>
