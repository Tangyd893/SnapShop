import http from './http'

export function getSeckillActivities(params?: { status?: string; pageNo?: number; pageSize?: number }) {
  return http.get('/seckill/activities', { params })
}

export function getSeckillActivityDetail(activityId: number) {
  return http.get(`/seckill/activities/${activityId}`)
}

export function getSeckillItemDetail(activityId: number, skuId: number) {
  return http.get(`/seckill/activities/${activityId}/items/${skuId}`)
}

export function getSeckillToken(activityId: number, skuId: number) {
  return http.post(`/seckill/activities/${activityId}/items/${skuId}/token`)
}

export function submitSeckill(activityId: number, skuId: number, seckillToken: string) {
  return http.post(`/seckill/activities/${activityId}/items/${skuId}/submit`, { seckillToken, quantity: 1 })
}

export function getSeckillResult(requestId: string) {
  return http.get(`/seckill/results/${requestId}`)
}
