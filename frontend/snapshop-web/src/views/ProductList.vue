<template>
  <div class="product-list">
    <h2>商品列表</h2>
    <div class="list">
      <div v-for="item in products" :key="item.productId" class="product-card" @click="goDetail(item.productId)">
        <h3>{{ item.title }}</h3>
        <p class="price">¥{{ (item.price / 100).toFixed(2) }}</p>
        <p :class="{ 'seckill-tag': item.seckillPrice }">
          {{ item.seckillPrice ? '秒杀价 ¥' + (item.seckillPrice / 100).toFixed(2) : '' }}
        </p>
      </div>
    </div>
    <p v-if="products.length === 0">暂无商品</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProducts } from '@/api/product'

const router = useRouter()
const products = ref<any[]>([])

onMounted(async () => {
  try {
    const res = await getProducts({})
    if (res.code === 0) {
      products.value = res.data.records
    }
  } catch (e) {
    // ignore
  }
})

function goDetail(productId: number) {
  router.push(`/products/${productId}`)
}
</script>

<style scoped>
.product-list { padding: 20px; }
.list { display: flex; flex-wrap: wrap; gap: 16px; }
.product-card {
  width: 200px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow .2s;
}
.product-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.1); }
.price { color: #f56c6c; font-size: 18px; font-weight: bold; margin: 8px 0; }
.seckill-tag { color: #e6a23c; font-size: 14px; }
</style>
