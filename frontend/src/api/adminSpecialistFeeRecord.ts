import request from './request'

export interface SpecialistFeeChangeRecord {
  id: number
  specialistId: number
  oldFee: number
  newFee: number
  level: string
  rangeMin: number
  rangeMax: number
  outOfRange: boolean
  changedByUserId?: number
  changedByName?: string
  createdAt: string
}

export function getSpecialistFeeChangeRecords(id: number) {
  return request({
    url: `/v1/admin/specialists/${id}/fee-change-records`,
    method: 'get'
  }) as Promise<SpecialistFeeChangeRecord[]>
}
