<template>
  <div class="product-edit">
    <el-card>
      <template #header>
        <span>{{ isCreate ? '新增商品' : '编辑商品' }}</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入商品标题" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
        </el-form-item>
        <el-form-item label="封面图" prop="mainImage">
          <el-input v-model="form.mainImage" placeholder="请输入图片URL" />
          <el-image
            v-if="form.mainImage"
            :src="form.mainImage"
            style="margin-top: 8px; width: 120px; height: 120px"
            fit="cover"
          />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 240px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">SKU 管理</el-divider>

        <div v-for="(sku, index) in form.skus" :key="index" class="sku-row">
          <el-row :gutter="12" align="middle">
            <el-col :span="5">
              <el-input v-model="sku.spec" placeholder="规格名称（如：黑色-64G）" />
            </el-col>
            <el-col :span="4">
              <el-input-number v-model="sku.price" :min="0" :precision="2" placeholder="售价" style="width: 100%" />
            </el-col>
            <el-col :span="4">
              <el-input-number v-model="sku.stock" :min="0" placeholder="库存" style="width: 100%" />
            </el-col>
            <el-col :span="4">
              <el-button type="danger" text @click="removeSku(index)">删除</el-button>
            </el-col>
          </el-row>
        </div>
        <el-button type="primary" dashed @click="addSku">+ 新增 SKU</el-button>

        <el-form-item style="margin-top: 24px">
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, createProduct, updateProduct } from '@/api/product'
import { getCategories } from '@/api/category'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const route = useRoute()
const router = useRouter()

const productId = route.params.id as string
const isCreate = computed(() => productId === 'new')

interface SkuItem {
  spec: string
  price: number | undefined
  stock: number | undefined
}

const form = reactive({
  title: '',
  description: '',
  mainImage: '',
  categoryId: null as number | null,
  skus: [] as SkuItem[]
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const categories = ref<{ id: number; name: string }[]>([])
const formRef = ref<FormInstance>()
const saving = ref(false)

onMounted(async () => {
  await fetchCategories()
  if (!isCreate.value) {
    await fetchDetail()
  } else {
    addSku()
  }
})

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch { /* ignore */ }
}

async function fetchDetail() {
  try {
    const res = await getProductDetail(Number(productId))
    const data = res.data
    form.title = data.title || ''
    form.description = data.description || ''
    form.mainImage = data.mainImage || ''
    form.categoryId = data.categoryId ?? null
    form.skus = (data.skus || []).map((s: any) => ({
      spec: s.spec || '',
      price: s.price ?? undefined,
      stock: s.stock ?? undefined
    }))
  } catch {
    ElMessage.error('获取商品详情失败')
  }
}

function addSku() {
  form.skus.push({ spec: '', price: undefined, stock: undefined })
}

function removeSku(index: number) {
  form.skus.splice(index, 1)
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      title: form.title,
      description: form.description,
      mainImage: form.mainImage,
      categoryId: form.categoryId,
      skus: form.skus.map(s => ({ spec: s.spec, price: s.price, stock: s.stock }))
    }
    if (isCreate.value) {
      await createProduct(payload)
      ElMessage.success('创建成功')
    } else {
      await updateProduct(Number(productId), payload)
      ElMessage.success('更新成功')
    }
    router.push('/products')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.sku-row {
  margin-bottom: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
}
</style>
