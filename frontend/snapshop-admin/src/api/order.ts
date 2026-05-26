import http from './http'

/** 分页查询订单列表 */
export function getOrders(params?: {
  status?: string
  userId?: number
  startTime?: string
  endTime?: string
  pageNo?: number
  pageSize?: number
}) {
  return http.get('/orders', { params })
}

/** 获取订单详情（含状态流水） */
export function getOrderDetail(id: number) {
  return http.get(`/orders/${id}`)
}
