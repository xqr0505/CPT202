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

export interface BookingListItem {
  id: string;
  specialistId: string;
  specialistName: string;
  specialistAvatar?: string;
  specialistTitle?: string;
  appointmentDateTime: string;
  serviceName: string;
  status: BookingStatus | string;
  amount?: number;
}

export interface BookingListResponse {
  total: number;
  list: BookingListItem[];
}

export interface BookingListQuery {
  pageNo?: number;
  pageSize?: number;
  tab?: 'UPCOMING' | 'HISTORY';
  status?: string;
}

export const getUpcomingBookings = () => {
  return request.get<UpcomingBookingResponse[]>('/api/v1/customer/dashboard/upcoming')
}

export const createBooking = (data: CreateBookingRequest) => {
  return request.post<CreateBookingResponse>('/api/v1/customer/bookings', data)
}

export const getBookingList = (params: BookingListQuery) => {
  return request.get<BookingListResponse>('/api/v1/customer/bookings/list', { params });
}
