import http from './http'

export function getProducts(params: { keyword?: string; categoryId?: number; pageNo?: number; pageSize?: number }) {
  return http.get('/products', { params })
}

export function getProductDetail(productId: number) {
  return http.get(`/products/${productId}`)
}

export function getCategories() {
  return http.get('/products/categories')
}
