<template>
  <div class="seckill-activity-list">
    <el-card>
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="活动名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="未开始" value="UPCOMING" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已结束" value="ENDED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button type="success" @click="handleCreate">新增活动</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="activityName" label="活动名称" min-width="180" />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleWarmup(row)">预热</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="pagination.pageNo"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchActivities"
          @size-change="fetchActivities"
        />
      </div>
    </el-card>

    <!-- 新增活动弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增秒杀活动"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px">
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="dialogForm.activityName" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="dialogForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="dialogForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
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
import { useRouter } from 'vue-router'
import { getSeckillActivities, createSeckillActivity, warmupSeckillActivity } from '@/api/seckill'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()

const queryForm = reactive({
  keyword: '',
  status: ''
})

const tableData = ref([])
const loading = ref(false)

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

// 新增弹窗
const dialogVisible = ref(false)
const dialogSaving = ref(false)
const dialogFormRef = ref<FormInstance>()
const dialogForm = reactive({
  activityName: '',
  startTime: '',
  endTime: ''
})

const dialogRules: FormRules = {
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

onMounted(() => fetchActivities())

async function fetchActivities() {
  loading.value = true
  try {
    const res = await getSeckillActivities({
      keyword: queryForm.keyword || undefined,
      status: queryForm.status || undefined,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchActivities()
}

function handleCreate() {
  dialogForm.activityName = ''
  dialogForm.startTime = ''
  dialogForm.endTime = ''
  dialogVisible.value = true
}

async function handleDialogSave() {
  const valid = await dialogFormRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogSaving.value = true
  try {
    await createSeckillActivity({
      activityName: dialogForm.activityName,
      startTime: dialogForm.startTime,
      endTime: dialogForm.endTime
    })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchActivities()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    dialogSaving.value = false
  }
}

function handleEdit(row: any) {
  router.push(`/seckill/activities/${row.id}`)
}

async function handleWarmup(row: any) {
  try {
    await warmupSeckillActivity(row.id)
    ElMessage.success('预热已触发')
  } catch {
    ElMessage.error('预热失败')
  }
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    UPCOMING: 'info',
    ACTIVE: 'success',
    ENDED: 'danger'
  }
  return map[status] || ''
}
</script>
