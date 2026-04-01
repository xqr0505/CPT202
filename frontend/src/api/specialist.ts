import request from './request'
import type {
  SpecialistAvailabilitySlot,
  SpecialistCategory,
  SpecialistDetail,
  SpecialistSearchParams,
  SpecialistSearchResult,
} from '@/types/specialist'

const buildSearchParams = (params: SpecialistSearchParams): Record<string, string | number> => {
  const query: Record<string, string | number> = {}

  if (params.keyword?.trim()) {
    query.keyword = params.keyword.trim()
  }
  if (typeof params.categoryId === 'number') {
    query.categoryId = params.categoryId
  }
  if (params.date) {
    query.date = params.date
  }
  if (params.sortBy) {
    query.sortBy = params.sortBy
  }
  if (params.pageNo) {
    query.pageNo = params.pageNo
  }
  if (params.pageSize) {
    query.pageSize = params.pageSize
  }

  return query
}

export const fetchSpecialistCategories = (): Promise<SpecialistCategory[]> => {
  return request.get('/api/v1/categories')
}

export const fetchSpecialists = (
  params: SpecialistSearchParams,
): Promise<SpecialistSearchResult> => {
  return request.get('/api/v1/specialists', {
    params: buildSearchParams(params),
  })
}

export const fetchSpecialistDetail = (id: number): Promise<SpecialistDetail> => {
  return request.get(`/api/v1/specialists/${id}`)
}

export const fetchSpecialistAvailability = (
  id: number,
  date: string,
): Promise<SpecialistAvailabilitySlot[]> => {
  return request.get(`/api/v1/specialists/${id}/availability`, {
    params: { date },
  })
}
