<template>
  <div class="home-page">
    <h1>SnapShop 秒杀平台</h1>
    <div class="nav-links">
      <router-link to="/products">商品列表</router-link>
      <router-link to="/seckill">秒杀活动</router-link>
      <router-link to="/orders">我的订单</router-link>
      <template v-if="userStore.isLoggedIn()">
        <span>欢迎，{{ userStore.nickname || userStore.username }}</span>
        <button @click="handleLogout">退出登录</button>
      </template>
      <router-link v-else to="/login">登录</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'

const userStore = useUserStore()

async function handleLogout() {
  try {
    await logoutApi()
  } catch (e) {
    // ignore
  }
  userStore.logout()
}
</script>

<style scoped>
.home-page {
  text-align: center;
  padding: 40px 20px;
}
.nav-links {
  margin-top: 20px;
  display: flex;
  gap: 16px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}
.nav-links a {
  color: #409eff;
  text-decoration: none;
  padding: 8px 16px;
}
</style>
