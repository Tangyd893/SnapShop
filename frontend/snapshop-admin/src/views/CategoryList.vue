<template>
  <div class="category-list">
    <el-card>
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="handleCreate">新增分类</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" row-key="id" default-expand-all stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleCreateSub(row)">新增子分类</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogForm.id ? '编辑分类' : '新增分类'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="dialogForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="dialogForm.parentId" placeholder="无（一级分类）" clearable style="width: 100%">
            <el-option v-for="c in topLevelCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dialogForm.sortOrder" :min="0" style="width: 100%" />
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
import { ref, reactive, onMounted, computed } from 'vue'
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const tableData = ref<{ id: number; name: string; sortOrder: number; parentId: number | null; children?: any[] }[]>([])
const loading = ref(false)

// 弹窗
const dialogVisible = ref(false)
const dialogSaving = ref(false)
const dialogFormRef = ref<FormInstance>()
const dialogForm = reactive({
  id: 0,
  name: '',
  parentId: null as number | null,
  sortOrder: 0
})

const dialogRules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

/** 只展示一级分类供上级选择 */
const topLevelCategories = computed(() => tableData.value.filter(c => c.parentId == null))

onMounted(() => fetchCategories())

async function fetchCategories() {
  loading.value = true
  try {
    const res = await getCategories()
    const raw = res.data || []
    // 构建树形结构
    const parents = raw.filter((c: any) => c.parentId == null)
    for (const p of parents) {
      p.children = raw.filter((c: any) => c.parentId === p.id)
    }
    tableData.value = parents
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  dialogForm.id = 0
  dialogForm.name = ''
  dialogForm.parentId = null
  dialogForm.sortOrder = 0
  dialogVisible.value = true
}

function handleCreateSub(row: any) {
  dialogForm.id = 0
  dialogForm.name = ''
  dialogForm.parentId = row.id
  dialogForm.sortOrder = 0
  dialogVisible.value = true
}

function handleEdit(row: any) {
  dialogForm.id = row.id
  dialogForm.name = row.name
  dialogForm.parentId = row.parentId ?? null
  dialogForm.sortOrder = row.sortOrder ?? 0
  dialogVisible.value = true
}

async function handleDialogSave() {
  const valid = await dialogFormRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogSaving.value = true
  try {
    const payload = {
      name: dialogForm.name,
      parentId: dialogForm.parentId ?? null,
      sortOrder: dialogForm.sortOrder
    }
    if (dialogForm.id) {
      await updateCategory(dialogForm.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createCategory(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchCategories()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    dialogSaving.value = false
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确定要删除分类「${row.name}」吗？`, '确认删除', { type: 'warning' })
    .then(async () => {
      await deleteCategory(row.id)
      ElMessage.success('删除成功')
      fetchCategories()
    })
    .catch(() => {})
}
</script>
