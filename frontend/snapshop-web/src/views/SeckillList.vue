<template>
  <div class="seckill-list">
    <h2>秒杀活动</h2>
    <div v-for="act in activities" :key="act.activityId" class="activity-card" @click="goDetail(act.activityId)">
      <h3>{{ act.activityName }}</h3>
      <p>{{ act.startTime }} ~ {{ act.endTime }}</p>
      <span :class="'status-' + (act.status === '进行中' ? 'running' : '')">{{ act.status }}</span>
    </div>
    <p v-if="activities.length === 0">暂无秒杀活动</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSeckillActivities } from '@/api/seckill'

const router = useRouter()
const activities = ref<any[]>([])

onMounted(async () => {
  try {
    const res = await getSeckillActivities()
    if (res.code === 0) {
      activities.value = res.data.records
    }
  } catch (e) {
    // ignore
  }
})

function goDetail(activityId: number) {
  router.push(`/seckill/${activityId}`)
}
</script>

<style scoped>
.seckill-list { padding: 20px; }
.activity-card {
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
}
.status-running { color: #67c23a; font-weight: bold; }
</style>
