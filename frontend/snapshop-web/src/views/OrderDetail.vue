<template>
  <div class="order-detail" v-if="order">
    <h2>订单详情</h2>
    <p>订单号：{{ order.orderNo }}</p>
    <p>金额：¥{{ (order.totalAmount / 100).toFixed(2) }}</p>
    <p>状态：{{ order.status }}</p>
    <p>创建时间：{{ order.createdAt }}</p>
    <div v-if="order.items" class="items">
      <h3>商品明细</h3>
      <div v-for="item in order.items" :key="item.skuId" class="item-card">
        <span>{{ item.title }}</span>
        <span>x{{ item.quantity }}</span>
        <span>¥{{ (item.price / 100).toFixed(2) }}</span>
      </div>
    </div>
  </div>
  <p v-else>加载中...</p>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail } from '@/api/order'

const route = useRoute()
const order = ref<any>(null)

onMounted(async () => {
  const orderId = Number(route.params.orderId)
  try {
    const res = await getOrderDetail(orderId)
    if (res.code === 0) {
      order.value = res.data
    }
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.order-detail { padding: 20px; }
.items { margin-top: 16px; }
.item-card { padding: 8px; display: flex; gap: 16px; background: #f5f5f5; border-radius: 4px; margin-bottom: 4px; }
</style>
