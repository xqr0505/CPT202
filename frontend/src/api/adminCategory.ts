import request from './request'

// Get category list
export function getCategoryList() {
  return request({
    url: '/v1/admin/categories',
    method: 'get'
  })
}

// Create category
export function createCategory(data: { categoryName: string }) {
  return request({
    url: '/v1/admin/categories',
    method: 'post',
    data
  })
}

// Update category
export function updateCategory(id: number, data: { categoryName: string }) {
  return request({
    url: `/v1/admin/categories/${id}`,
    method: 'put',
    data
  })
}

// Delete category
export function deleteCategory(id: number) {
  return request({
    url: `/v1/admin/categories/${id}`,
    method: 'delete'
  })
}
