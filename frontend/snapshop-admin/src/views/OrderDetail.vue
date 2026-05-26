<template>
  <div class="order-detail">
    <el-card v-loading="loading">
      <template #header>
        <span>订单详情 #{{ order.id }}</span>
        <el-button style="float: right" @click="router.back()">返回</el-button>
      </template>

      <!-- 基本信息 -->
      <el-descriptions title="基本信息" :column="2" border>
        <el-descriptions-item label="订单编号">{{ order.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ order.userId }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="statusTagType(order.status)">{{ order.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ order.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ order.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createdAt }}</el-descriptions-item>
      </el-descriptions>

      <!-- 商品明细 -->
      <el-descriptions title="商品明细" :column="1" border style="margin-top: 20px">
        <el-descriptions-item v-for="item in order.items" :key="item.skuId">
          {{ item.productTitle }} / {{ item.skuSpec || '-' }} × {{ item.quantity }}
          ￥{{ item.price }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 状态流水 -->
      <div style="margin-top: 20px">
        <h4 style="margin-bottom: 12px">状态流水</h4>
        <el-timeline v-if="order.statusLogs && order.statusLogs.length">
          <el-timeline-item
            v-for="log in order.statusLogs"
            :key="log.id"
            :timestamp="log.createdAt"
            placement="top"
          >
            {{ log.fromStatus || '-' }} → {{ log.toStatus }}
            <span v-if="log.remark" style="color: #909399; margin-left: 8px">{{ log.remark }}</span>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无状态记录" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/order'

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id as string)

const order = reactive({
  id: 0,
  userId: 0,
  status: '',
  totalAmount: 0,
  address: '',
  createdAt: '',
  items: [] as any[],
  statusLogs: [] as any[]
})

const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(orderId)
    Object.assign(order, res.data || {})
  } finally {
    loading.value = false
  }
})

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
