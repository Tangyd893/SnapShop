import axios from 'axios'
import { useAdminStore } from '@/stores/admin'
import { generateRequestId } from '@/utils/requestId'

const http = axios.create({
  baseURL: '/api/admin',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：注入 admin token 和 requestId
http.interceptors.request.use(
  (config) => {
    const adminStore = useAdminStore()
    if (adminStore.adminToken) {
      config.headers.Authorization = `Bearer ${adminStore.adminToken}`
    }
    if (config.method === 'post' || config.method === 'put' || config.method === 'delete') {
      config.headers['X-Request-Id'] = generateRequestId()
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
http.interceptors.response.use(
  (response) => {
    const data = response.data
    // 401 或 token 失效，跳转登录页
    if (data.code === 40100 || data.code === 40101) {
      const adminStore = useAdminStore()
      adminStore.logout()
      window.location.href = '/login'
    }
    return data
  },
  (error) => {
    if (error.response?.status === 401) {
      const adminStore = useAdminStore()
      adminStore.logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default http
