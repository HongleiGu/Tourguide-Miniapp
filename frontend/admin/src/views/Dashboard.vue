<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPing } from '@/api/ping'
import type { PingResponse } from '@/api/types'

const ping = ref<PingResponse | null>(null)
const error = ref('')
const loading = ref(true)

onMounted(async () => {
  try {
    ping.value = await getPing()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header>后端连通性</template>
          <div v-loading="loading" class="conn">
            <template v-if="ping">
              <el-tag type="success">在线</el-tag>
              <p>服务：{{ ping.service }}</p>
              <p>Profiles：{{ ping.profiles?.join(', ') }}</p>
              <p class="time">{{ ping.time }}</p>
            </template>
            <el-tag v-else-if="error" type="danger">离线：{{ error }}</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>今日订单</template>
          <div class="stat">—</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>讲解员在岗</template>
          <div class="stat">—</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="welcome">
      <h3>欢迎使用景区讲解服务管理后台</h3>
      <p>
        这是 MIN-15 的脚手架页面。业务模块（人员管理、订单与场次、拼团规则、考核、数据大屏）
        将随后续 epic 落地。统计卡片现为占位数据。
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.conn p {
  margin: 6px 0 0;
  font-size: 13px;
}
.time {
  color: #909399;
}
.stat {
  font-size: 28px;
  font-weight: 600;
  color: #1f3a5f;
}
.welcome h3 {
  margin-top: 0;
}
</style>
