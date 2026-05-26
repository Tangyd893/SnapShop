import http from './http'

export function login(account: string, password: string) {
  return http.post('/auth/login', { account, password })
}

export function register(username: string, password: string, phone: string) {
  return http.post('/auth/register', { username, password, phone })
}

export function logout() {
  return http.post('/auth/logout')
}
