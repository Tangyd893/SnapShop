<template>
  <div class="product-detail" v-if="product">
    <h2>{{ product.title }}</h2>
    <p class="price">¥{{ (product.price / 100).toFixed(2) }}</p>
    <p>{{ product.description }}</p>
    <template v-if="product.skus">
      <div v-for="sku in product.skus" :key="sku.skuId" class="sku-item">
        <span>{{ sku.skuName }}</span>
        <span>¥{{ (sku.price / 100).toFixed(2) }}</span>
      </div>
    </template>
  </div>
  <p v-else>加载中...</p>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProductDetail } from '@/api/product'

const route = useRoute()
const product = ref<any>(null)

onMounted(async () => {
  const productId = Number(route.params.productId)
  try {
    const res = await getProductDetail(productId)
    if (res.code === 0) {
      product.value = res.data
    }
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.product-detail { padding: 20px; }
.price { color: #f56c6c; font-size: 24px; font-weight: bold; }
.sku-item { padding: 8px; margin: 4px 0; background: #f5f5f5; border-radius: 4px; display: flex; justify-content: space-between; }
</style>
