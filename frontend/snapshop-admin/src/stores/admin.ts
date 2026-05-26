import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAdminStore = defineStore('admin', () => {
  const adminToken = ref<string>(localStorage.getItem('adminToken') || '')
  const adminRole = ref<string>(localStorage.getItem('adminRole') || '')
  const username = ref<string>(localStorage.getItem('adminUsername') || '')

  const isLoggedIn = computed(() => !!adminToken.value)

  /** 是否为超级管理员 */
  const isSuperAdmin = computed(() => adminRole.value === 'SUPER_ADMIN')

  /** 是否为运营或以上角色 */
  const isOperatorPlus = computed(() =>
    ['SUPER_ADMIN', 'OPERATOR'].includes(adminRole.value)
  )

  function setLoginInfo(token: string, role: string, name: string) {
    adminToken.value = token
    adminRole.value = role
    username.value = name
    localStorage.setItem('adminToken', token)
    localStorage.setItem('adminRole', role)
    localStorage.setItem('adminUsername', name)
  }

  function logout() {
    adminToken.value = ''
    adminRole.value = ''
    username.value = ''
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminRole')
    localStorage.removeItem('adminUsername')
  }

  return {
    adminToken,
    adminRole,
    username,
    isLoggedIn,
    isSuperAdmin,
    isOperatorPlus,
    setLoginInfo,
    logout
  }
})
