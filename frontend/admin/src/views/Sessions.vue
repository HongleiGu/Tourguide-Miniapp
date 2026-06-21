<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createSession,
  listSessions,
  SESSION_STATUS_LABELS,
  SESSION_TYPE_LABELS,
  SESSION_TYPE_OPTIONS,
  setSessionStatus,
  updateSession,
  type AdminSession,
  type SessionInput,
} from '@/api/sessions'

const loading = ref(false)
const sessions = ref<AdminSession[]>([])
const filterDate = ref<string>('')
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

interface SessionForm {
  title: string
  type: string
  date: string
  startTime: string
  endTime: string
  capacity: number
  priceYuan: number
  guideId: number | null
  groupMinSize: number
  groupMaxSize: number
}

const blank = (): SessionForm => ({
  title: '',
  type: 'PRIVATE',
  date: '',
  startTime: '',
  endTime: '',
  capacity: 1,
  priceYuan: 0,
  guideId: null,
  groupMinSize: 2,
  groupMaxSize: 10,
})

const form = reactive<SessionForm>(blank())
const isGroup = computed(() => form.type === 'GROUP')

async function load() {
  loading.value = true
  try {
    sessions.value = await listSessions(filterDate.value || undefined)
  } finally {
    loading.value = false
  }
}
onMounted(load)

function openCreate() {
  Object.assign(form, blank())
  editingId.value = null
  dialogVisible.value = true
}

function openEdit(row: AdminSession) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    type: row.type,
    date: row.date,
    startTime: row.startTime ?? '',
    endTime: row.endTime ?? '',
    capacity: row.capacity,
    priceYuan: row.priceFen / 100,
    guideId: row.guideId,
    groupMinSize: row.groupMin ?? 2,
    groupMaxSize: row.groupMax ?? 10,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.title.trim() || !form.date) {
    ElMessage.warning('请填写名称和日期')
    return
  }
  const payload: SessionInput = {
    title: form.title.trim(),
    type: form.type,
    date: form.date,
    startTime: form.startTime || undefined,
    endTime: form.endTime || undefined,
    capacity: form.capacity ?? undefined,
    priceFen: Math.round(form.priceYuan * 100),
    guideId: form.guideId ?? undefined,
    groupMinSize: form.groupMinSize ?? undefined,
    groupMaxSize: form.groupMaxSize ?? undefined,
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateSession(editingId.value, payload)
    } else {
      await createSession(payload)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function changeStatus(row: AdminSession, status: string) {
  await setSessionStatus(row.id, status)
  ElMessage.success('已更新')
  await load()
}
</script>

<template>
  <div>
    <div class="toolbar">
      <el-date-picker
        v-model="filterDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="按日期筛选"
        clearable
        @change="load"
      />
      <el-button type="primary" @click="openCreate">加场</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="sessions" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="名称" min-width="160" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ SESSION_TYPE_LABELS[row.type] ?? row.type }}</template>
      </el-table-column>
      <el-table-column label="日期/时间" min-width="170">
        <template #default="{ row }">
          {{ row.date }} {{ row.startTime ?? '' }}<template v-if="row.endTime">-{{ row.endTime }}</template>
        </template>
      </el-table-column>
      <el-table-column label="容量/拼团" width="120">
        <template #default="{ row }">
          <span v-if="row.type === 'GROUP'">{{ row.groupCurrent ?? 0 }}/{{ row.groupMax ?? row.capacity }}（≥{{ row.groupMin ?? 2 }}）</span>
          <span v-else>{{ row.capacity }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单价" width="100">
        <template #default="{ row }">¥{{ (row.priceFen / 100).toFixed(0) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">{{ SESSION_STATUS_LABELS[row.status] ?? row.status }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== 'LOCKED'" size="small" type="warning" @click="changeStatus(row, 'LOCKED')">锁场</el-button>
          <el-button v-if="row.status !== 'OPEN'" size="small" type="success" @click="changeStatus(row, 'OPEN')">开场</el-button>
          <el-button v-if="row.status !== 'CLOSED'" size="small" type="danger" @click="changeStatus(row, 'CLOSED')">停场</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑场次' : '加场'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" :disabled="!!editingId">
            <el-option v-for="o in SESSION_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="时间">
          <el-time-picker v-model="form.startTime" value-format="HH:mm" placeholder="开始" />
          <span style="margin: 0 8px">-</span>
          <el-time-picker v-model="form.endTime" value-format="HH:mm" placeholder="结束" />
        </el-form-item>
        <el-form-item label="单价(元)"><el-input-number v-model="form.priceYuan" :min="0" /></el-form-item>
        <el-form-item v-if="!isGroup" label="容量"><el-input-number v-model="form.capacity" :min="1" /></el-form-item>
        <template v-else>
          <el-form-item label="成团最低"><el-input-number v-model="form.groupMinSize" :min="1" /></el-form-item>
          <el-form-item label="最大人数"><el-input-number v-model="form.groupMaxSize" :min="1" /></el-form-item>
        </template>
        <el-form-item label="讲解员ID"><el-input-number v-model="form.guideId" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
