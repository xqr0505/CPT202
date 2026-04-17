import request from './request'
import type {
  SpecialistAvailabilitySlot,
  SpecialistCategory,
  SpecialistDetail,
  SpecialistSearchParams,
  SpecialistSearchResult,
} from '@/types/specialist'

const normalizeId = (value: unknown): number | undefined => {
  if (value === null || value === undefined || value === '') {
    return undefined
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const normalizeCategoryId = (value: unknown): number | undefined => {
  return normalizeId(value)
}

const buildSearchParams = (params: SpecialistSearchParams): Record<string, string | number> => {
  const query: Record<string, string | number> = {}
  const categoryId = normalizeCategoryId(params.categoryId)

  if (params.keyword?.trim()) {
    query.keyword = params.keyword.trim()
  }
  if (categoryId !== undefined) {
    query.categoryId = categoryId
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

export const fetchSpecialistCategories = async (): Promise<SpecialistCategory[]> => {
  const categories = (await request.get('/api/v1/categories')) as Array<
    SpecialistCategory & { id: unknown }
  >

  return categories.map((category) => ({
    ...category,
    id: normalizeId(category.id) ?? 0,
  }))
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
