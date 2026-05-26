<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px">
      <div class="logo">SnapShop 管理后台</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>

        <el-sub-menu v-if="adminStore.isOperatorPlus" index="product-sub">
          <template #title>
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </template>
          <el-menu-item index="/products">商品列表</el-menu-item>
          <el-menu-item index="/categories">分类管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="adminStore.isOperatorPlus" index="seckill-sub">
          <template #title>
            <el-icon><Timer /></el-icon>
            <span>秒杀管理</span>
          </template>
          <el-menu-item index="/seckill/activities">活动列表</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>

        <el-menu-item v-if="adminStore.isSuperAdmin" index="/mq/dead-letters">
          <el-icon><Warning /></el-icon>
          <span>死信管理</span>
        </el-menu-item>

        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item v-if="adminStore.isSuperAdmin" index="/admins">
          <el-icon><Avatar /></el-icon>
          <span>管理员管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="topbar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="topbar-right">
          <el-tag>{{ adminStore.adminRole }}</el-tag>
          <span class="username">{{ adminStore.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { DataAnalysis, Goods, Timer, Document, Warning, User, Avatar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { logout as apiLogout } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/products')) return '/products'
  if (path.startsWith('/seckill/activities')) return '/seckill/activities'
  if (path.startsWith('/orders')) return '/orders'
  if (path.startsWith('/mq')) return '/mq/dead-letters'
  return path
})

async function handleLogout() {
  try {
    await apiLogout()
  } finally {
    adminStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.el-aside {
  background-color: #304156;
  overflow-y: auto;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.username {
  color: #606266;
}
.el-main {
  background: #f0f2f5;
  overflow-y: auto;
}
</style>
