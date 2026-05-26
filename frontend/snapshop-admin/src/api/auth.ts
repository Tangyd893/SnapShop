import http from './http'

/** 管理员登录 */
export function login(username: string, password: string) {
  return http.post('/auth/login', { username, password })
}

/** 管理员退出 */
export function logout() {
  return http.post('/auth/logout')
}
