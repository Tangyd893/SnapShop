<template>
  <div class="seckill-activity-edit">
    <el-card>
      <template #header>
        <span>活动配置</span>
      </template>

      <!-- 活动基本信息 -->
      <el-form ref="formRef" :model="form" :rules="activityRules" label-width="100px">
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="form.activityName" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存活动</el-button>
          <el-button type="warning" :loading="warming" @click="handleWarmup">触发预热</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>绑定商品</span>
      </template>

      <el-table :data="bindItems" stripe>
        <el-table-column prop="skuId" label="SKU ID" width="100" />
        <el-table-column prop="skuSpec" label="规格" width="150" />
        <el-table-column prop="seckillPrice" label="秒杀价" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.seckillPrice"
              :min="0"
              :precision="2"
              size="small"
              style="width: 120px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="seckillStock" label="秒杀库存" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.seckillStock" :min="0" size="small" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeBindItem($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 12px">
        <el-button type="primary" dashed @click="showProductPicker = true">+ 选择商品</el-button>
        <el-button type="primary" :loading="binding" @click="handleBindItems">保存绑定</el-button>
      </div>
    </el-card>

    <!-- 商品选择器弹窗 -->
    <el-dialog v-model="showProductPicker" title="选择商品" width="700px">
      <el-input v-model="productSearch" placeholder="搜索商品..." clearable style="margin-bottom: 12px" />
      <el-table
        :data="productList"
        v-loading="productLoading"
        @selection-change="onProductSelect"
        height="300px"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="商品名称" min-width="150" />
        <el-table-column label="SKU" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="sku in row.skus" :key="sku.id" style="margin: 2px" size="small">
              {{ sku.spec }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showProductPicker = false">取消</el-button>
        <el-button type="primary" @click="confirmProductPicker">确定选择</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSeckillActivityDetail, updateSeckillActivity, bindSeckillItems, warmupSeckillActivity } from '@/api/seckill'
import { getProducts } from '@/api/product'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activityId = Number(route.params.id as string)

// 活动信息
const formRef = ref<FormInstance>()
const form = reactive({
  activityName: '',
  startTime: '',
  endTime: ''
})

const activityRules: FormRules = {
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const saving = ref(false)
const warming = ref(false)
const binding = ref(false)

// 绑定商品
interface BindItem {
  skuId: number | null
  skuSpec: string
  seckillPrice: number | undefined
  seckillStock: number | undefined
}

const bindItems = ref<BindItem[]>([])

// 商品选择器
const showProductPicker = ref(false)
const productSearch = ref('')
const productList = ref<any[]>([])
const productLoading = ref(false)
const selectedProducts = ref<any[]>([])

onMounted(() => fetchActivityDetail())

async function fetchActivityDetail() {
  try {
    const res = await getSeckillActivityDetail(activityId)
    const data = res.data
    form.activityName = data.activityName || ''
    form.startTime = data.startTime || ''
    form.endTime = data.endTime || ''
    bindItems.value = (data.items || []).map((i: any) => ({
      skuId: i.skuId,
      skuSpec: i.skuSpec || `SKU#${i.skuId}`,
      seckillPrice: i.seckillPrice ?? undefined,
      seckillStock: i.seckillStock ?? undefined
    }))
  } catch {
    ElMessage.error('获取活动详情失败')
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateSeckillActivity(activityId, {
      activityName: form.activityName,
      startTime: form.startTime,
      endTime: form.endTime
    })
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleWarmup() {
  warming.value = true
  try {
    await warmupSeckillActivity(activityId)
    ElMessage.success('预热已触发，Redis 库存键已写入')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '预热失败')
  } finally {
    warming.value = false
  }
}

async function handleBindItems() {
  binding.value = true
  try {
    await bindSeckillItems(activityId, {
      items: bindItems.value.map(i => ({
        skuId: i.skuId,
        seckillPrice: i.seckillPrice,
        seckillStock: i.seckillStock
      }))
    })
    ElMessage.success('绑定成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '绑定失败')
  } finally {
    binding.value = false
  }
}

function removeBindItem(index: number) {
  bindItems.value.splice(index, 1)
}

function onProductSelect(selection: any[]) {
  selectedProducts.value = selection
}

async function confirmProductPicker() {
  for (const product of selectedProducts.value) {
    for (const sku of product.skus || []) {
      // 避免重复添加
      if (bindItems.value.some(b => b.skuId === sku.id)) continue
      bindItems.value.push({
        skuId: sku.id,
        skuSpec: sku.spec || `SKU#${sku.id}`,
        seckillPrice: undefined,
        seckillStock: undefined
      })
    }
  }
  showProductPicker.value = false
}
</script>
