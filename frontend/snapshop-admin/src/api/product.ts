import http from './http'

/** 分页查询商品列表 */
export function getProducts(params: {
  keyword?: string
  categoryId?: number
  status?: string
  pageNo?: number
  pageSize?: number
}) {
  return http.get('/products', { params })
}

/** 获取商品详情 */
export function getProductDetail(id: number) {
  return http.get(`/products/${id}`)
}

/** 创建商品 */
export function createProduct(data: any) {
  return http.post('/products', data)
}

/** 更新商品 */
export function updateProduct(id: number, data: any) {
  return http.put(`/products/${id}`, data)
}

/** 商品上下架 */
export function updateProductStatus(id: number, status: string) {
  return http.put(`/products/${id}/status`, { status })
}
