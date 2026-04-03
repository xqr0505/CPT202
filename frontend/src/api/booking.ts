import request from './request'
import type {BookingStatus} from "@/constants/booking.ts";

export interface UpcomingBookingResponse {
  id: string;
  specialistName: string;
  serviceName: string;
  startTime: string; // format: 'YYYY-MM-DD HH:mm:ss'
  today: boolean;
  status: string;
}

export interface CreateBookingRequest {
  specialistId: number
  slotId: number
  topic: string
  customerNotes?: string
}

export interface CreateBookingResponse {
  bookingId: number
  status: BookingStatus | string
}

export interface BookingHistoryItem {
  id: number
  specialistName: string
  specialistTitle: string
  specialistAvatar: string
  startTime: string
  endTime: string
  duration: number
  status: BookingStatus | string
}

export interface BookingHistoryResponse {
  total: number
  list: BookingHistoryItem[]
}

export interface UnifiedBookingItem {
  id: string
  specialistName: string
  specialistAvatar?: string
  specialistTitle?: string
  startTime: string
  endTime: string
  serviceName: string
  status: BookingStatus | string
  amount: number
  originalBookingId?: string
}

export interface UnifiedBookingResponse {
  total: number
  list: UnifiedBookingItem[]
}

export const getUpcomingBookings = () => {
  return request.get<UpcomingBookingResponse[]>('/api/v1/customer/dashboard/upcoming')
}

export const createBooking = (data: CreateBookingRequest) => {
  return request.post<CreateBookingResponse>('/api/v1/customer/bookings', data)
}

export const getBookingHistory = (params: {
  pageNo: number
  pageSize: number
  timeScope?: string
  status?: string
}) => {
  return request.get<BookingHistoryResponse>('/api/v1/customer/bookings', {
    params,
  })
}

export const getUnifiedBookings = (params: {
  pageNo: number
  pageSize: number
  type: 'upcoming' | 'history'
}) => {
  return request.get<UnifiedBookingResponse>('/api/v1/customer/bookings/unified', {
    params,
  })
}
