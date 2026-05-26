import http from './http'

/** 获取死信消息列表 */
export function getDeadLetters(params?: { pageNo?: number; pageSize?: number }) {
  return http.get('/mq/dead-letters', { params })
}

/** 死信重投 */
export function requeueDeadLetter(id: number) {
  return http.post(`/mq/dead-letters/${id}/requeue`)
}
