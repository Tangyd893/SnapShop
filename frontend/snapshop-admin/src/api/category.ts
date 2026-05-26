import http from './http'

/** 获取分类列表 */
export function getCategories() {
  return http.get('/categories')
}

/** 创建分类 */
export function createCategory(data: { name: string; parentId?: number | null; sortOrder?: number }) {
  return http.post('/categories', data)
}

/** 更新分类 */
export function updateCategory(id: number, data: { name?: string; sortOrder?: number }) {
  return http.put(`/categories/${id}`, data)
}

/** 删除分类 */
export function deleteCategory(id: number) {
  return http.delete(`/categories/${id}`)
}
