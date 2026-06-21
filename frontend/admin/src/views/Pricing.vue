<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DAY_LABELS, listPricing, TYPE_LABELS, upsertPricing, type PricingRule } from '@/api/pricing'

interface Row extends PricingRule {
  priceYuan: number
}

const loading = ref(false)
const rows = ref<Row[]>([])
const savingId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    rows.value = (await listPricing()).map((r) => ({ ...r, priceYuan: r.priceFen / 100 }))
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function save(row: Row) {
  savingId.value = row.id
  try {
    await upsertPricing({
      sessionType: row.sessionType,
      dayType: row.dayType,
      priceFen: Math.round(row.priceYuan * 100),
      groupMin: row.sessionType === 'GROUP' ? row.groupMin : null,
      groupMax: row.sessionType === 'GROUP' ? row.groupMax : null,
    })
    ElMessage.success('已保存')
    await load()
  } finally {
    savingId.value = null
  }
}
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="hint">私讲 / 拼团 / 专属时段 的工作日与节假日两套价格；拼团另设成团最低与最大人数。</span>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ TYPE_LABELS[row.sessionType] ?? row.sessionType }}</template>
      </el-table-column>
      <el-table-column label="日期类型" width="120">
        <template #default="{ row }">{{ DAY_LABELS[row.dayType] ?? row.dayType }}</template>
      </el-table-column>
      <el-table-column label="单价(元)" width="180">
        <template #default="{ row }">
          <el-input-number v-model="row.priceYuan" :min="0" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="成团最低" width="160">
        <template #default="{ row }">
          <el-input-number v-if="row.sessionType === 'GROUP'" v-model="row.groupMin" :min="1" size="small" />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="最大人数" width="160">
        <template #default="{ row }">
          <el-input-number v-if="row.sessionType === 'GROUP'" v-model="row.groupMax" :min="1" size="small" />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" :loading="savingId === row.id" @click="save(row)">
            保存
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.hint {
  color: #909399;
  font-size: 13px;
}
</style>
