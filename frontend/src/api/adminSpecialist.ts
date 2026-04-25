import request from './request'

export type SpecialistStatus = 'Active' | 'Inactive'

export interface SpecialistItem {
  id: number
  name: string
  email?: string
  categoryId: number
  categoryName?: string
  level: string
  consultationFee: number
  status: SpecialistStatus
  hasActiveBookings?: boolean
  activeBookingCount?: number
  avatarUrl?: string
  createTime?: string
}

export interface SpecialistListParams {
  keyword?: string
  categoryId?: number
  status?: SpecialistStatus
  pageNo?: number
  pageSize?: number
}

export interface SpecialistListResponse {
  total: number
  list: SpecialistItem[]
}

export interface CreateSpecialistPayload {
  name: string
  email: string
  categoryId: number
  level: string
  consultationFee: number
  status: SpecialistStatus
  avatarUrl?: string
}

export interface UpdateSpecialistPayload {
  name: string
  email: string
  password?: string
  resetPasswordToDefault?: boolean
  categoryId: number
  level: string
  consultationFee: number
  status: SpecialistStatus
  avatarUrl?: string
}

export interface SpecialistLevelOption {
  value: string
  label: string
  minFee: number
  maxFee: number
}

export function getSpecialistList(params?: SpecialistListParams) {
  return request({
    url: '/api/v1/admin/specialists',
    method: 'get',
    params
  }) as Promise<SpecialistListResponse>
}

export function getSpecialistLevels() {
  return request({
    url: '/api/v1/admin/specialists/levels',
    method: 'get'
  }) as Promise<SpecialistLevelOption[]>
}

export function getSpecialistDetail(id: number) {
  return request({
    url: `/api/v1/admin/specialists/${id}`,
    method: 'get'
  }) as Promise<SpecialistItem>
}

export function createSpecialist(data: CreateSpecialistPayload) {
  return request({
    url: '/api/v1/admin/specialists',
    method: 'post',
    data
  })
}

export function updateSpecialist(id: number, data: UpdateSpecialistPayload) {
  return request({
    url: `/api/v1/admin/specialists/${id}`,
    method: 'put',
    data
  })
}

export function updateSpecialistStatus(id: number, status: SpecialistStatus) {
  return request({
    url: `/api/v1/admin/specialists/${id}/status`,
    method: 'patch',
    data: { status }
  }) as Promise<number>
}
