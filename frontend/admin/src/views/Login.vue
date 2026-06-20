<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { adminLogin, fetchMe } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const loading = ref(false)

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const tokens = await adminLogin(form.username, form.password)
    auth.setTokens(tokens.accessToken ?? '', tokens.refreshToken ?? '')
    const me = await fetchMe()
    auth.setProfile(form.username, me.roles ?? [])
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/')
  } catch {
    // Error toast is shown by the axios interceptor.
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <el-card class="card">
      <h2 class="brand">景区讲解服务后台</h2>
      <p class="subtitle">管理员登录</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="admin" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit" native-type="submit">
          登录
        </el-button>
      </el-form>
      <p class="hint">开发环境默认账号：admin / admin123</p>
    </el-card>
  </div>
</template>

<style scoped>
.login {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3a5f, #2c5282);
}
.card {
  width: 360px;
}
.brand {
  margin: 4px 0 0;
  text-align: center;
  color: #1f3a5f;
}
.subtitle {
  margin: 4px 0 16px;
  text-align: center;
  color: #909399;
}
.submit {
  width: 100%;
}
.hint {
  margin-top: 12px;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}
</style>
