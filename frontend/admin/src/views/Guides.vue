<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createGuide,
  EMPLOYMENT_OPTIONS,
  GUIDE_STATUS_LABELS,
  listGuides,
  setGuideEmployment,
  setGuideEnabled,
  setGuideSuspended,
  setGuideWeight,
  type AdminGuide,
} from '@/api/guides'

const loading = ref(false)
const guides = ref<AdminGuide[]>([])
const dialogVisible = ref(false)
const creating = ref(false)
const form = reactive({ name: '', employmentType: 'SELF' })

async function load() {
  loading.value = true
  try {
    guides.value = await listGuides()
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function submitCreate() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  creating.value = true
  try {
    await createGuide(form.name.trim(), form.employmentType)
    ElMessage.success('已创建')
    dialogVisible.value = false
    form.name = ''
    form.employmentType = 'SELF'
    await load()
  } finally {
    creating.value = false
  }
}

function statusType(status: string): 'success' | 'info' | 'danger' {
  if (status === 'ENABLED') return 'success'
  if (status === 'SUSPENDED') return 'danger'
  return 'info'
}

async function onEmployment(row: AdminGuide, value: string) {
  await setGuideEmployment(row.guideId, value)
  ElMessage.success('已更新')
}

async function onWeight(row: AdminGuide, value: number) {
  await setGuideWeight(row.guideId, value)
  ElMessage.success('权重已更新')
}

async function onToggleEnabled(row: AdminGuide) {
  const enabled = row.status !== 'ENABLED'
  await setGuideEnabled(row.guideId, enabled)
  await load()
}

async function onToggleSuspend(row: AdminGuide) {
  const suspend = row.status !== 'SUSPENDED'
  await setGuideSuspended(row.guideId, suspend)
  await load()
}
</script>

<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="dialogVisible = true">新增讲解员</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="guides" v-loading="loading" border stripe>
      <el-table-column prop="guideId" label="ID" width="70" />
      <el-table-column prop="name" label="姓名" min-width="120" />
      <el-table-column label="类型" width="130">
        <template #default="{ row }">
          <el-select
            :model-value="row.employmentType"
            size="small"
            @change="(v: string) => onEmployment(row, v)"
          >
            <el-option v-for="o in EMPLOYMENT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ GUIDE_STATUS_LABELS[row.status] ?? row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接单" width="80">
        <template #default="{ row }">{{ row.acceptingOrders ? '开' : '关' }}</template>
      </el-table-column>
      <el-table-column label="派单权重" width="140">
        <template #default="{ row }">
          <el-input-number
            :model-value="row.dispatchWeight"
            :min="0"
            size="small"
            @change="(v: number) => onWeight(row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="90" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onToggleEnabled(row)">
            {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="warning" @click="onToggleSuspend(row)">
            {{ row.status === 'SUSPENDED' ? '恢复' : '暂停' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增讲解员" width="420px">
      <el-form label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="讲解员姓名" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.employmentType">
            <el-option v-for="o in EMPLOYMENT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
