<template>
  <div class="login-page">
    <h2>用户登录</h2>
    <form @submit.prevent="handleLogin">
      <div class="form-item">
        <label>账号</label>
        <input v-model="form.account" type="text" placeholder="请输入用户名或手机号" />
      </div>
      <div class="form-item">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </div>
      <button type="submit">登录</button>
    </form>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const errorMsg = ref('')
const form = reactive({ account: '', password: '' })

async function handleLogin() {
  errorMsg.value = ''
  try {
    const res = await login(form.account, form.password)
    if (res.code === 0) {
      const d = res.data
      userStore.setLoginInfo(d.accessToken, d.refreshToken, d.user)
      router.push('/')
    } else {
      errorMsg.value = res.message
    }
  } catch (e: any) {
    errorMsg.value = e?.message || '网络错误'
  }
}
</script>

<style scoped>
.login-page {
  max-width: 400px;
  margin: 60px auto;
  padding: 30px;
  background: #fff;
  border-radius: 8px;
}
h2 { text-align: center; margin-bottom: 20px; }
.form-item { margin-bottom: 16px; }
.form-item label { display: block; margin-bottom: 4px; }
.form-item input { width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; }
button { width: 100%; padding: 10px; background: #409eff; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.error { color: #f56c6c; margin-top: 10px; }
</style>
