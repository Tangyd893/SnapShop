import http from './http'

export function getOrders(params?: { status?: string; pageNo?: number; pageSize?: number }) {
  return http.get('/orders', { params })
}

export function getOrderDetail(orderId: number) {
  return http.get(`/orders/${orderId}`)
}

export function cancelOrder(orderId: number, reason: string) {
  return http.post(`/orders/${orderId}/cancel`, { reason })
}
