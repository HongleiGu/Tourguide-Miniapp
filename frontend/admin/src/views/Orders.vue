<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  exportOrders,
  handleOrder,
  listOrders,
  ORDER_STATUS_LABELS,
  ORDER_STATUS_OPTIONS,
  ORDER_TYPE_LABELS,
  ORDER_TYPE_OPTIONS,
  type AdminOrder,
  type OrderFilters,
} from '@/api/orders'

const loading = ref(false)
const orders = ref<AdminOrder[]>([])
const dateRange = ref<[string, string] | null>(null)
const filters = reactive<OrderFilters>({})

const HANDLE_ACTIONS = [
  { command: 'CANCEL', label: '取消订单' },
  { command: 'REFUND', label: '退款' },
  { command: 'COMPLETE', label: '强制完成' },
]

function buildFilters(): OrderFilters {
  return {
    status: filters.status || undefined,
    type: filters.type || undefined,
    guideId: filters.guideId || undefined,
    from: dateRange.value?.[0],
    to: dateRange.value?.[1],
  }
}

async function load() {
  loading.value = true
  try {
    orders.value = await listOrders(buildFilters())
  } finally {
    loading.value = false
  }
}
onMounted(load)

function reset() {
  filters.status = undefined
  filters.type = undefined
  filters.guideId = undefined
  dateRange.value = null
  load()
}

async function doExport() {
  await exportOrders(buildFilters())
}

async function onHandle(row: AdminOrder, action: string) {
  const { value: reason } = await ElMessageBox.prompt('请输入处理原因', '异常处理', {
    inputPlaceholder: '原因（可选）',
  })
  await handleOrder(row.id, action, reason ?? '')
  ElMessage.success('已处理')
  await load()
}
</script>

<template>
  <div>
    <div class="toolbar">
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px">
        <el-option v-for="o in ORDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="filters.type" placeholder="类型" clearable style="width: 130px">
        <el-option v-for="o in ORDER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-input v-model.number="filters.guideId" placeholder="讲解员ID" clearable style="width: 120px" />
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="出行起"
        end-placeholder="出行止"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <el-button type="success" @click="doExport">导出 Excel</el-button>
    </div>

    <el-table :data="orders" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ ORDER_TYPE_LABELS[row.type] ?? row.type }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">{{ ORDER_STATUS_LABELS[row.status] ?? row.status }}</template>
      </el-table-column>
      <el-table-column prop="peopleCount" label="人数" width="70" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ (row.amountFen / 100).toFixed(0) }}</template>
      </el-table-column>
      <el-table-column prop="visitDate" label="出行日期" width="120" />
      <el-table-column prop="sessionTitle" label="场次" min-width="140" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-dropdown @command="(cmd: string) => onHandle(row, cmd)">
            <el-button size="small">异常处理</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="a in HANDLE_ACTIONS" :key="a.command" :command="a.command">
                  {{ a.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
