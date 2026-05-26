import http from './http'

/** 获取秒杀活动列表 */
export function getSeckillActivities(params?: {
  status?: string
  keyword?: string
  pageNo?: number
  pageSize?: number
}) {
  return http.get('/seckill/activities', { params })
}

/** 获取秒杀活动详情 */
export function getSeckillActivityDetail(id: number) {
  return http.get(`/seckill/activities/${id}`)
}

/** 创建秒杀活动 */
export function createSeckillActivity(data: any) {
  return http.post('/seckill/activities', data)
}

/** 更新秒杀活动 */
export function updateSeckillActivity(id: number, data: any) {
  return http.put(`/seckill/activities/${id}`, data)
}

/** 绑定秒杀商品 */
export function bindSeckillItems(activityId: number, data: any) {
  return http.post(`/seckill/activities/${activityId}/items`, data)
}

/** 触发活动预热 */
export function warmupSeckillActivity(activityId: number) {
  return http.post(`/seckill/activities/${activityId}/warmup`)
}
