<template>
  <div class="admin-list">
    <el-card>
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="handleCreate">新增管理员</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="role" label="角色" width="140">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.status === 'ACTIVE' && row.id !== currentAdminId"
              title="确定要禁用该管理员吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDisable(row)"
            >
              <template #reference>
                <el-button type="danger" link>禁用</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              v-if="row.status === 'DISABLED'"
              title="确定要重新启用该管理员吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleEnable(row)"
            >
              <template #reference>
                <el-button type="success" link>启用</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增管理员弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增管理员"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="dialogForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="dialogForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="dialogForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="超级管理员" value="SUPER_ADMIN" />
            <el-option label="运营" value="OPERATOR" />
            <el-option label="客服" value="SUPPORT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSaving" @click="handleDialogSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const currentAdminId = 0 // 当前登录管理员ID，避免自己禁用自己

// 新增弹窗
const dialogVisible = ref(false)
const dialogSaving = ref(false)
const dialogFormRef = ref<FormInstance>()
const dialogForm = reactive({
  username: '',
  password: '',
  role: 'OPERATOR'
})

const dialogRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度3-32位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

onMounted(() => fetchAdmins())

async function fetchAdmins() {
  loading.value = true
  try {
    const res = await http.get('/admins')
    tableData.value = res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  dialogForm.username = ''
  dialogForm.password = ''
  dialogForm.role = 'OPERATOR'
  dialogVisible.value = true
}

async function handleDialogSave() {
  const valid = await dialogFormRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogSaving.value = true
  try {
    await http.post('/auth/register', {
      username: dialogForm.username,
      password: dialogForm.password,
      role: dialogForm.role
    })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchAdmins()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    dialogSaving.value = false
  }
}

async function handleDisable(row: any) {
  try {
    await http.put(`/admins/${row.id}/status`, { status: 'DISABLED' })
    row.status = 'DISABLED'
    ElMessage.success('已禁用')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleEnable(row: any) {
  try {
    await http.put(`/admins/${row.id}/status`, { status: 'ACTIVE' })
    row.status = 'ACTIVE'
    ElMessage.success('已启用')
  } catch {
    ElMessage.error('操作失败')
  }
}

function roleTagType(role: string) {
  const map: Record<string, string> = {
    SUPER_ADMIN: 'danger',
    OPERATOR: 'primary',
    SUPPORT: 'info'
  }
  return map[role] || ''
}
</script>
