import request from './request'
import type { BookingStatus } from '@/constants/booking'

export interface UpcomingBookingResponse {
  id: number
  specialistName: string
  specialistTitle: string
  startTime: string // ISO string
  endTime: string // ISO string
  status: BookingStatus
}

export const getUpcomingBookings = () => {
  return request.get<UpcomingBookingResponse[]>('/api/v1/customer/dashboard/upcoming')
}
