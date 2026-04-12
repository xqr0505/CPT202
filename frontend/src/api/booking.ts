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

export type BookingItem = BookingListItem;

export interface ConsultedExpertSummary {
  specialistId: string;
  specialistName: string;
  specialistAvatar?: string;
}

export interface CustomerDashboardSummary {
  totalCompletedAppointments: number;
  totalAmountSpent: number;
  totalConsultationHours: number;
  consultedExperts: ConsultedExpertSummary[];
}

export interface CustomerDashboardSummaryQuery {
  startDate?: string;
  endDate?: string;
}

export interface BookingDetail {
  bookingId: number;
  status: string;
  specialistId: number;
  specialistName: string;
  specialistAvatar: string;
  slotDate: string;
  startTime: string;
  endTime: string;
  price: number;
  topic: string;
  customerNotes: string;
}

export const getUpcomingBookings = () => {
  return request.get<UpcomingBookingResponse[]>('/api/v1/customer/dashboard/upcoming')
}

export const getCustomerDashboardSummary = (params?: CustomerDashboardSummaryQuery) => {
  return request
    .get<CustomerDashboardSummary>('/api/v1/customer/dashboard/summary', { params })
    .then((response) => response as unknown as CustomerDashboardSummary)
}
export const getBookingTopics = (): Promise<string[]> => {
  return request.get<any, string[]>('/api/v1/booking-topics')
}

export const createBooking = (data: CreateBookingRequest, suppressErrorMessage = false) => {
  return request.post<any, CreateBookingResponse>('/api/v1/customer/bookings', data, suppressErrorMessage
    ? ({ suppressErrorMessage: true } as any)
    : undefined)
}

export const getBookingList = (params: BookingListQuery) => {
  return request.get<BookingListResponse>('/api/v1/customer/bookings/list', { params });
}

export const getBookingDetail = (bookingId: number | string) => {
  return request.get<BookingDetail>(`/api/v1/customer/bookings/${bookingId}`);
}
