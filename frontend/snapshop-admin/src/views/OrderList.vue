<template>
  <div class="order-list">
    <el-card>
      <!-- 筛选栏 -->
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="订单状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已发货" value="SHIPPED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELED" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="queryForm.userId" placeholder="用户ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <el-table :data="tableData" v-loading="loading" stripe @row-click="handleRowClick" style="cursor: pointer">
        <el-table-column prop="id" label="订单编号" width="160" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="handleViewDetail(row)">
              详情
            </el-button>
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
          @current-change="fetchOrders"
          @size-change="fetchOrders"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders } from '@/api/order'

const router = useRouter()

const queryForm = reactive({
  status: '',
  userId: null as number | null,
  dateRange: null as [string, string] | null
})

const tableData = ref([])
const loading = ref(false)

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const params: any = {
      status: queryForm.status || undefined,
      userId: queryForm.userId ?? undefined,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    }
    if (queryForm.dateRange) {
      params.startTime = queryForm.dateRange[0]
      params.endTime = queryForm.dateRange[1]
    }
    const res = await getOrders(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchOrders()
}

function handleReset() {
  queryForm.status = ''
  queryForm.userId = null
  queryForm.dateRange = null
  handleSearch()
}

function handleRowClick(row: any) {
  router.push(`/orders/${row.id}`)
}

function handleViewDetail(row: any) {
  router.push(`/orders/${row.id}`)
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    PENDING_PAY: 'warning',
    PAID: 'primary',
    SHIPPED: 'success',
    COMPLETED: 'info',
    CANCELED: 'danger'
  }
  return map[status] || ''
}
</script>
