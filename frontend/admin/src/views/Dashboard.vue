<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPing } from '@/api/ping'
import { getStats, type Stats } from '@/api/stats'
import type { PingResponse } from '@/api/types'

const router = useRouter()

const ping = ref<PingResponse | null>(null)
const pingError = ref('')
const stats = ref<Stats | null>(null)
const loading = ref(true)

onMounted(async () => {
  getPing()
    .then((p) => (ping.value = p))
    .catch((e) => (pingError.value = (e as Error).message))
  try {
    stats.value = await getStats()
  } finally {
    loading.value = false
  }
})

const cards = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    { label: '订单总量', value: s.totalOrders },
    { label: '待核销', value: s.paidOrders },
    { label: '已完成', value: s.completedOrders },
    { label: '游客量', value: s.visitors },
    { label: '营收总额(元)', value: (s.revenueFen / 100).toFixed(2) },
    { label: '成团率', value: `${(s.groupFormationRate * 100).toFixed(1)}%` },
    { label: '核销率', value: `${(s.verificationRate * 100).toFixed(1)}%` },
  ]
})

const quickLinks = [
  { label: '人员管理', path: '/guides' },
  { label: '场次管理', path: '/sessions' },
  { label: '订单管理', path: '/orders' },
  { label: '基础统计', path: '/stats' },
]
</script>

<template>
  <div class="dashboard">
    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="c in cards" :key="c.label" :span="6" class="card-col">
        <el-card shadow="hover">
          <div class="card-label">{{ c.label }}</div>
          <div class="card-value">{{ c.value }}</div>
        </el-card>
      </el-col>
      <el-col v-if="!loading && !cards.length" :span="24">
        <el-card><el-empty description="暂无统计数据" /></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card>
          <template #header>快捷入口</template>
          <el-button v-for="l in quickLinks" :key="l.path" class="quick" @click="router.push(l.path)">
            {{ l.label }}
          </el-button>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>后端连通性</template>
          <template v-if="ping">
            <el-tag type="success">在线</el-tag>
            <p>服务：{{ ping.service }}</p>
            <p class="muted">{{ ping.time }}</p>
          </template>
          <el-tag v-else-if="pingError" type="danger">离线：{{ pingError }}</el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
.quick {
  margin: 0 12px 12px 0;
}
.muted {
  color: #909399;
  font-size: 13px;
  margin: 6px 0 0;
}
</style>
