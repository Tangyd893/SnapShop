import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { generateRequestId } from '@/utils/requestId'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

http.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    if (config.method === 'post' || config.method === 'put') {
      config.headers['X-Request-Id'] = generateRequestId()
    }
    return config
  },
  (error) => Promise.reject(error)
)

http.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code === 40100) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    }
    return data
  },
  (error) => {
    return Promise.reject(error)
  }
)

export default http
