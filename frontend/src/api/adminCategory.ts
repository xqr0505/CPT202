import request from './request'

// 获取分类列表
export function getCategoryList() {
  return request({
    url: '/admin/categories',
    method: 'get'
  })
}

// 创建分类
export function createCategory(data: { categoryName: string }) {
  return request({
    url: '/admin/categories',
    method: 'post',
    data
  })
}

// 更新分类
export function updateCategory(id: number, data: { categoryName: string }) {
  return request({
    url: `/admin/categories/${id}`,
    method: 'put',
    data
  })
}

// 删除分类
export function deleteCategory(id: number) {
  return request({
    url: `/admin/categories/${id}`,
    method: 'delete'
  })
}
