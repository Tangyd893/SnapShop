<template>
  <div class="dead-letter-list">
    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="messageId" label="消息ID" width="200" show-overflow-tooltip />
        <el-table-column prop="queueName" label="队列" width="140" />
        <el-table-column prop="messageBody" label="消息体" min-width="200" show-overflow-tooltip />
        <el-table-column prop="errorMessage" label="异常信息" min-width="180" show-overflow-tooltip />
        <el-table-column prop="retryCount" label="重试次数" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column v-if="adminStore.isSuperAdmin" label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              title="确定要重新投递该消息吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleRequeue(row)"
            >
              <template #reference>
                <el-button type="primary" link>重投</el-button>
              </template>
            </el-popconfirm>
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
          @current-change="fetchDeadLetters"
          @size-change="fetchDeadLetters"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { getDeadLetters, requeueDeadLetter } from '@/api/mq'
import { ElMessage } from 'element-plus'

const adminStore = useAdminStore()

const tableData = ref([])
const loading = ref(false)

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

onMounted(() => fetchDeadLetters())

async function fetchDeadLetters() {
  loading.value = true
  try {
    const res = await getDeadLetters({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function handleRequeue(row: any) {
  try {
    await requeueDeadLetter(row.id)
    ElMessage.success('重投成功')
    fetchDeadLetters()
  } catch {
    ElMessage.error('重投失败')
  }
}
</script>
