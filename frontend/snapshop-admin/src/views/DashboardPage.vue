<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <el-statistic title="今日订单数" :value="stats.todayOrders">
            <template #suffix>
              <el-tag size="small" type="primary">实时</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="秒杀成功率" :value="stats.seckillSuccessRate" :precision="2">
            <template #suffix>
              <span style="font-size: 16px">%</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="MQ 堆积数" :value="stats.mqPendingCount">
            <template #suffix>
              <el-tag v-if="stats.mqPendingCount > 100" size="small" type="danger">告警</el-tag>
              <el-tag v-else size="small" type="success">正常</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>待处理</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="待处理订单">{{ stats.pendingOrders }}</el-descriptions-item>
            <el-descriptions-item label="死信消息">{{ stats.deadLetterCount }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>进行中秒杀活动</span></template>
          <el-empty v-if="activeActivities.length === 0" description="暂无进行中的活动" />
          <el-tag v-for="a in activeActivities" :key="a.id" style="margin: 4px" type="success">
            {{ a.activityName || `活动 #${a.id}` }}
          </el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import http from '@/api/http'

const stats = reactive({
  todayOrders: 0,
  seckillSuccessRate: 0,
  mqPendingCount: 0,
  pendingOrders: 0,
  deadLetterCount: 0
})

const activeActivities = reactive<{ id: number; activityName: string }[]>([])

onMounted(async () => {
  try {
    const res = await http.get('/dashboard')
    if (res.data) {
      Object.assign(stats, res.data)
      if (res.data.activeActivities) {
        activeActivities.push(...res.data.activeActivities)
      }
    }
  } catch {
    // 后端接口未实现时，展示默认数据
  }
})
</script>
