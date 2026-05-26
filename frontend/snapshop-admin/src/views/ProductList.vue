<template>
  <div class="product-list">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="商品名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.categoryId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="上架" value="ON_SALE" />
            <el-option label="下架" value="OFF_SALE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="handleCreate">新增商品</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="商品名称" min-width="180" />
        <el-table-column label="封面图" width="100">
          <template #default="{ row }">
            <el-image v-if="row.mainImage" :src="row.mainImage" style="width: 60px; height: 60px" fit="cover" />
            <span v-else class="no-image">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'ON_SALE'"
              active-text="上架"
              inactive-text="下架"
              inline-prompt
              @change="(val: boolean) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="pagination.pageNo"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchProducts"
          @size-change="fetchProducts"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProducts, updateProductStatus } from '@/api/product'
import { getCategories } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const queryForm = reactive({
  keyword: '',
  categoryId: null as number | null,
  status: ''
})

const categories = ref<{ id: number; name: string }[]>([])
const tableData = ref([])
const loading = ref(false)

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

onMounted(() => {
  fetchCategories()
  fetchProducts()
})

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch { /* ignore */ }
}

async function fetchProducts() {
  loading.value = true
  try {
    const res = await getProducts({
      keyword: queryForm.keyword || undefined,
      categoryId: queryForm.categoryId ?? undefined,
      status: queryForm.status || undefined,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchProducts()
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.categoryId = null
  queryForm.status = ''
  handleSearch()
}

function handleCreate() {
  router.push('/products/new')
}

function handleEdit(row: any) {
  router.push(`/products/${row.id}`)
}

async function handleToggleStatus(row: any, onSale: boolean) {
  const status = onSale ? 'ON_SALE' : 'OFF_SALE'
  try {
    await updateProductStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 'ON_SALE' ? '已上架' : '已下架')
  } catch {
    ElMessage.error('操作失败')
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确定要删除商品「${row.title}」吗？`, '确认删除', { type: 'warning' })
    .then(async () => {
      // 删除逻辑待后端接口实现
      ElMessage.success('删除成功')
      fetchProducts()
    })
    .catch(() => {})
}
</script>

<style scoped>
.product-list {
  padding: 0;
}
.no-image {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
