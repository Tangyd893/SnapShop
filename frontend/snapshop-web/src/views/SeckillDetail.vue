<template>
  <div class="seckill-detail" v-if="detail">
    <h2>{{ detail.activityName }}</h2>
    <p>{{ detail.startTime }} ~ {{ detail.endTime }}</p>
    <p :class="{ 'running': detail.status === '进行中' }">{{ detail.status }}</p>
    <div v-if="detail.items" class="items">
      <div v-for="item in detail.items" :key="item.skuId" class="item-card">
        <h3>{{ item.title }}</h3>
        <p class="origin-price">原价：¥{{ (item.originPrice / 100).toFixed(2) }}</p>
        <p class="seckill-price">秒杀价：¥{{ (item.seckillPrice / 100).toFixed(2) }}</p>
        <button
          :disabled="buttonDisabled"
          @click="handleSeckill(item.skuId)"
        >{{ buttonText }}</button>
      </div>
    </div>
    <p v-if="resultMsg" class="result-msg">{{ resultMsg }}</p>
  </div>
  <p v-else>加载中...</p>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSeckillActivityDetail, getSeckillToken, submitSeckill, getSeckillResult } from '@/api/seckill'
import { useUserStore } from '@/stores/user'
import { useSeckillStore } from '@/stores/seckill'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const seckillStore = useSeckillStore()
const detail = ref<any>(null)
const submitting = ref(false)
const resultMsg = ref('')

const buttonDisabled = computed(() => !userStore.isLoggedIn() || submitting.value)
const buttonText = computed(() => {
  if (!userStore.isLoggedIn()) return '请先登录'
  if (submitting.value) return '提交中...'
  if (detail.value?.status !== '进行中') return '活动未开始'
  return '立即抢购'
})

async function handleSeckill(skuId: number) {
  if (submitting.value) return
  submitting.value = true
  resultMsg.value = ''
  try {
    const activityId = Number(route.params.activityId)
    // 获取秒杀令牌
    const tokenRes = await getSeckillToken(activityId, skuId)
    if (tokenRes.code !== 0) {
      resultMsg.value = tokenRes.message
      submitting.value = false
      return
    }
    const token = tokenRes.data.seckillToken
    // 提交秒杀
    const submitRes = await submitSeckill(activityId, skuId, token)
    if (submitRes.code === 60005) {
      resultMsg.value = '排队中...'
      const reqId = submitRes.data.requestId
      // 轮询结果
      let pollCount = 0
      const poll = setInterval(async () => {
        pollCount++
        const pollRes = await getSeckillResult(reqId)
        if (pollRes.code === 0) {
          const d = pollRes.data
          if (d.resultStatus === '成功') {
            clearInterval(poll)
            resultMsg.value = '抢购成功！'
            router.push(`/orders/${d.orderId}`)
          } else if (d.resultStatus === '失败' || d.resultStatus === '售罄' || d.resultStatus === '重复参与') {
            clearInterval(poll)
            resultMsg.value = d.resultStatus === '售罄' ? '已售罄' : d.resultStatus === '重复参与' ? '已参与过' : '抢购失败'
            submitting.value = false
          } else if (pollCount >= 15) {
            clearInterval(poll)
            resultMsg.value = '处理中，请稍后在订单列表查看'
            submitting.value = false
          }
        }
      }, 2000)
    } else {
      resultMsg.value = submitRes.message
    }
  } catch (e: any) {
    resultMsg.value = e?.message || '网络错误'
  }
  submitting.value = false
}

onMounted(async () => {
  const activityId = Number(route.params.activityId)
  try {
    const res = await getSeckillActivityDetail(activityId)
    if (res.code === 0) detail.value = res.data
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.seckill-detail { padding: 20px; }
.running { color: #67c23a; font-weight: bold; }
.items { margin-top: 16px; }
.item-card { padding: 16px; margin-bottom: 12px; background: #fff; border-radius: 8px; }
.origin-price { color: #999; text-decoration: line-through; }
.seckill-price { color: #f56c6c; font-size: 20px; font-weight: bold; }
button { margin-top: 8px; padding: 10px 24px; background: #f56c6c; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
button:disabled { background: #ddd; cursor: not-allowed; }
.result-msg { margin-top: 16px; padding: 10px; background: #fff3cd; border-radius: 4px; }
</style>
