<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { exportStats, getStats, type Stats } from '@/api/stats'

const loading = ref(false)
const stats = ref<Stats | null>(null)
const dateRange = ref<[string, string] | null>(null)

async function load() {
  loading.value = true
  try {
    stats.value = await getStats(dateRange.value?.[0], dateRange.value?.[1])
  } finally {
    loading.value = false
  }
}
onMounted(load)

const cards = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    { label: '订单量', value: s.totalOrders },
    { label: '待核销', value: s.paidOrders },
    { label: '已完成', value: s.completedOrders },
    { label: '游客量', value: s.visitors },
    { label: '营收总额(元)', value: (s.revenueFen / 100).toFixed(2) },
    { label: '成团率', value: `${(s.groupFormationRate * 100).toFixed(1)}%` },
    { label: '核销率', value: `${(s.verificationRate * 100).toFixed(1)}%` },
  ]
})

async function doExport() {
  await exportStats(dateRange.value?.[0], dateRange.value?.[1])
}
</script>

<template>
  <div v-loading="loading">
    <div class="toolbar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始"
        end-placeholder="结束"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="doExport">导出 Excel</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="c in cards" :key="c.label" :span="6" class="card-col">
        <el-card shadow="hover">
          <div class="card-label">{{ c.label }}</div>
          <div class="card-value">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
}
.card-col {
  margin-bottom: 16px;
}
.card-label {
  color: #909399;
  font-size: 13px;
}
.card-value {
  font-size: 26px;
  font-weight: 600;
  color: #1f3a5f;
  margin-top: 8px;
}
</style>
