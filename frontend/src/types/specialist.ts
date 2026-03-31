export const SPECIALIST_SORT_OPTIONS = {
  RECOMMENDED: 'recommended',
  LEVEL_DESC: 'levelDesc',
  FEE_ASC: 'feeAsc',
  FEE_DESC: 'feeDesc',
} as const

export type SpecialistSortOption =
  typeof SPECIALIST_SORT_OPTIONS[keyof typeof SPECIALIST_SORT_OPTIONS]

export interface SpecialistCategory {
  id: number
  name: string
}

export interface SpecialistSearchForm {
  keyword: string
  categoryId: number | null
  date: string
  sortBy: SpecialistSortOption
}

export interface SpecialistSearchParams {
  keyword?: string
  categoryId?: number
  date?: string
  sortBy?: SpecialistSortOption
  pageNo?: number
  pageSize?: number
}

export interface SpecialistSummary {
  id: number
  userId: number
  name: string
  avatarUrl: string
  categoryId: number | null
  categoryName: string
  level: string
  consultationFee: number
  bio: string
  status: string
  hasAvailabilityOnSelectedDate: boolean
}

export interface SpecialistDetail {
  id: number
  userId: number
  name: string
  avatarUrl: string
  categoryId: number | null
  categoryName: string
  level: string
  consultationFee: number
  bio: string
  status: string
  email: string
  phoneNumber: string
}

export interface SpecialistAvailabilitySlot {
  id: number
  slotDate: string
  startTime: string
  endTime: string
  status: string
}

export interface SpecialistSearchResult {
  total: number
  list: SpecialistSummary[]
}
