<template>
  <div class="order-list">
    <h2>我的订单</h2>
    <div v-for="order in orders" :key="order.orderId" class="order-card" @click="goDetail(order.orderId)">
      <h3>订单号：{{ order.orderNo }}</h3>
      <p>金额：¥{{ (order.totalAmount / 100).toFixed(2) }}</p>
      <span :class="'status-' + order.status">{{ order.status }}</span>
      <p>{{ order.createdAt }}</p>
    </div>
    <p v-if="orders.length === 0">暂无订单</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders } from '@/api/order'

const router = useRouter()
const orders = ref<any[]>([])

onMounted(async () => {
  try {
    const res = await getOrders()
    if (res.code === 0) {
      orders.value = res.data.records
    }
  } catch (e) {
    // ignore
  }
})

function goDetail(orderId: number) {
  router.push(`/orders/${orderId}`)
}
</script>

<style scoped>
.order-list { padding: 20px; }
.order-card {
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
}
</style>
