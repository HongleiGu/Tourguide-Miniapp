<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createAdmin,
  listAdmins,
  ROLE_LABELS,
  ROLE_OPTIONS,
  setAdminRoles,
  type AdminUser,
} from '@/api/admins'

const loading = ref(false)
const admins = ref<AdminUser[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ username: '', password: '', roles: [] as string[] })

const rolesDialog = ref(false)
const editing = ref<AdminUser | null>(null)
const editRoles = ref<string[]>([])

async function load() {
  loading.value = true
  try {
    admins.value = await listAdmins()
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function submitCreate() {
  if (!form.username.trim() || !form.password || !form.roles.length) {
    ElMessage.warning('请填写用户名、密码并选择角色')
    return
  }
  saving.value = true
  try {
    await createAdmin(form.username.trim(), form.password, form.roles)
    ElMessage.success('已创建')
    dialogVisible.value = false
    form.username = ''
    form.password = ''
    form.roles = []
    await load()
  } finally {
    saving.value = false
  }
}

function openRoles(row: AdminUser) {
  editing.value = row
  editRoles.value = [...row.roles]
  rolesDialog.value = true
}

async function saveRoles() {
  if (!editing.value || !editRoles.value.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  saving.value = true
  try {
    await setAdminRoles(editing.value.id, editRoles.value)
    ElMessage.success('已更新')
    rolesDialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="dialogVisible = true">新增管理员</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="admins" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column label="角色" min-width="240">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" class="role-tag">{{ ROLE_LABELS[r] ?? r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openRoles(row)">分配角色</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增管理员" width="440px">
      <el-form label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roles" multiple style="width: 100%">
            <el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rolesDialog" title="分配角色" width="440px">
      <el-select v-model="editRoles" multiple style="width: 100%">
        <el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <template #footer>
        <el-button @click="rolesDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.role-tag {
  margin-right: 6px;
}
</style>
