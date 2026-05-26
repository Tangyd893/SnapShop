import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string>(localStorage.getItem('accessToken') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userId = ref<number>(Number(localStorage.getItem('userId')) || 0)
  const username = ref<string>(localStorage.getItem('username') || '')
  const nickname = ref<string>(localStorage.getItem('nickname') || '')

  const isLoggedIn = () => !!accessToken.value

  const setLoginInfo = (token: string, refresh: string, user: { userId: number; username: string; nickname: string }) => {
    accessToken.value = token
    refreshToken.value = refresh
    userId.value = user.userId
    username.value = user.username
    nickname.value = user.nickname
    localStorage.setItem('accessToken', token)
    localStorage.setItem('refreshToken', refresh)
    localStorage.setItem('userId', String(user.userId))
    localStorage.setItem('username', user.username)
    localStorage.setItem('nickname', user.nickname)
  }

  const logout = () => {
    accessToken.value = ''
    refreshToken.value = ''
    userId.value = 0
    username.value = ''
    nickname.value = ''
    localStorage.clear()
  }

  return { accessToken, refreshToken, userId, username, nickname, isLoggedIn, setLoginInfo, logout }
})
