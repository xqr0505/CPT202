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

export interface DashboardTrendItem {
  dateLabel: string;
  count: number;
  hours: number;
}

export interface DashboardCategoryItem {
  categoryName: string;
  amount: number;
  count: number;
}

export interface DashboardHabitItem {
  dayOfWeek: string;
  count: number;
}

export interface DashboardStatistics {
  totalCompletedAppointments: number;
  totalAmountSpent: number;
  totalConsultationHours: number;
  consultedExperts: ConsultedExpertSummary[];
  trendData: DashboardTrendItem[];
  categoryData: DashboardCategoryItem[];
  habitData: DashboardHabitItem[];
}

export interface DashboardStatisticsQuery {
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

export interface BookingCancelQuote {
  allowed: boolean;
  reasonCode?: string; //only used when allowed is false
  message?: string;
  policyType?: string;
  bookingStartAt?: string;
  orderAmount?: number;
  refundAmount?: number;
  penaltyAmount?: number;
}

export interface BookingCancelConfirm {
  bookingId: number;
  bookingStatus: string;
  policyType?: string;
  refundAmount?: number;
  penaltyAmount?: number;
  message?: string;
}

export interface BookingRescheduleQuote {
  allowed: boolean;
  reasonCode?: string;
  message?: string;
  policyType?: string;
  bookingStartAt?: string;
  originalPrice?: number;
  newPrice?: number;
  priceDifference?: number;
  penaltyAmount?: number;
  refundAmount?: number;
  payableAmount?: number;
}

export interface BookingRescheduleConfirm {
  bookingId: number;
  bookingStatus: string;
  policyType?: string;
  priceDifference?: number;
  penaltyAmount?: number;
  refundAmount?: number;
  payableAmount?: number;
  message?: string;
}

export const getUpcomingBookings = () => {
  return request.get<UpcomingBookingResponse[]>('/api/v1/customer/dashboard/upcoming')
}

export const getCustomerDashboardSummary = (params?: CustomerDashboardSummaryQuery) => {
  return request
    .get<CustomerDashboardSummary>('/api/v1/customer/dashboard/summary', { params })
    .then((response) => response as unknown as CustomerDashboardSummary)
}

export const getCustomerDashboardStatistics = (params?: DashboardStatisticsQuery) => {
  return request
    .get<DashboardStatistics>('/api/v1/customer/dashboard/statistics', { params })
    .then((response) => response as unknown as DashboardStatistics)
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

export const getBookingCancelQuote = (bookingId: number | string) => {
  return request.post<any, BookingCancelQuote>(`/api/v1/customer/bookings/${bookingId}/cancel/quote`);
}

export const confirmBookingCancel = (bookingId: number | string) => {
  return request.post<any, BookingCancelConfirm>(`/api/v1/customer/bookings/${bookingId}/cancel/confirm`);
}

export const getBookingRescheduleQuote = (bookingId: number | string, newSlotId: number | string) => {
  return request.post<any, BookingRescheduleQuote>(`/api/v1/customer/bookings/${bookingId}/reschedule/quote`, null, {params: { newSlotId },} as any);
}

export const confirmBookingReschedule = (bookingId: number | string, newSlotId: number | string) => {
  return request.post<any, BookingRescheduleConfirm>(`/api/v1/customer/bookings/${bookingId}/reschedule/confirm`, null, {params: { newSlotId },} as any);
}
